package com.oneangrybean.proto.mixintegration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.webflux.dsl.WebFlux;
import org.springframework.messaging.Message;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

@SpringBootApplication
public class MixIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(MixIntegrationApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(final RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.basicAuthentication("api", "s3cr3t")
                                    .build();
    }

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Bean
    public IntegrationFlow getMixEntryFlowReactive(final RestTemplate restTemplate) {
        // return f->f.handle(
        //    Http.outboundGateway("http://localhost:8080/mix-entry/name/{mixEntryName}", restTemplate)
        //        .httpMethod(HttpMethod.GET)
        //        .uriVariable("mixEntryName", "payload")
        //        .expectedResponseType(String.class),
        //    e -> e.advice(new NotFoundRequestHandlerAdvice())
        // 	);
        final WebClient webClient =
                            webClientBuilder.defaultHeaders(
                                            headers -> headers.setBasicAuth("api", "s3cr3t")
                                        )
                                        .build();
        final String defaultPayload = "{ \"id\": -1, \"name\": null }";
        return f->
                f.enrich(
                    e -> e.requestSubFlow(
                        sf -> sf.handle(
                            WebFlux.outboundGateway("http://localhost:8080/mix-entry/name/{mixEntryName}", webClient)
                            .httpMethod(HttpMethod.GET)
                            .uriVariable("mixEntryName", "payload")
                            .expectedResponseType(String.class)
                            ,
                            ec -> ec.customizeMonoReply(
                                (Message<?> message, Mono<?> mono) -> 
                                    mono.onErrorResume(
                                        WebClientResponseException.class,
                                        ex1 -> Mono.just(ex1)
                                                    .filter(ex -> ex.getStatusCode() == HttpStatus.NOT_FOUND)
                                                    .map(ex -> defaultPayload)
                                                    .switchIfEmpty((Mono)mono)
                                    )
                            )
                        )
                        .logAndReply()
                    )
                    .headerExpression("mix-entry-id", "#jsonPath(payload, '$.id')")
            )
            .logAndReply();
    }

    @Bean
    public MessagingTemplate messagingTemplate() {
        return new MessagingTemplate();
    }

    @Bean
    public CommandLineRunner runner(final MessagingTemplate messagingTemplate) {
        return (args) -> {
            System.out.println();
            System.out.println();
            final String result = messagingTemplate.convertSendAndReceive("getMixEntryFlowReactive.input", "Two", String.class);
            System.out.println(result);
            System.out.println();
            System.out.println();
        };
    }
}
