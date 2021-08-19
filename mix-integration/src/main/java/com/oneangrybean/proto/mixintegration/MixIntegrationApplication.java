package com.oneangrybean.proto.mixintegration;

import java.io.FileWriter;
import java.io.PrintWriter;

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
import org.springframework.integration.http.dsl.Http;
import org.springframework.integration.webflux.dsl.WebFlux;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.MessageBuilder;
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
    public IntegrationFlow getMixEntryFlow(final RestTemplate restTemplate) {
        return f->f.handle(
           Http.outboundGateway("http://localhost:8080/mix-entry/name/{mixEntryName}", restTemplate)
               .httpMethod(HttpMethod.GET)
               .uriVariable("mixEntryName", "payload")
               .expectedResponseType(String.class),
           e -> e.advice(new NotFoundRequestHandlerAdvice())
        );
    }

    /**
     * Reactive based integration flow to request entity by name then populate mix-entry-id header
     * with ID of returned entity.
     * 
     * In the case of an HTTP/404, a default payload of { "id": -1, "name": null } will be returned.
     * 
     * This implementation adds an endpoint configuration attempting to customize the Mono reply
     * when a WebClientResponseException.NotFound is detected.
     * 
     * @return the integration flow
     */
    @Bean
    public IntegrationFlow getMixEntryFlowReactiveNotFound() {
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
                                (message, mono) -> 
                                    mono.onErrorResume(
                                        WebClientResponseException.NotFound.class,
                                        ex1 -> Mono.just(defaultPayload)
                                    )
                            )
                        )
                        .logAndReply()
                    )
                    .headerExpression("mix-entry-id", "#jsonPath(payload, '$.id')")
            )
            .logAndReply();
    }

    /**
     * Reactive based integration flow to request entity by name then populate mix-entry-id header
     * with ID of returned entity.
     * 
     * In the case of an HTTP/404, a default payload of { "id": -1, "name": null } will be returned.
     * 
     * This implementation adds an endpoint configuration attempting to customize the Mono reply
     * when a WebClientResponseException is detected and checks for a status code of NOT_FOUND (404).
     * 
     * @return the integration flow
     */
    @Bean
    public IntegrationFlow getMixEntryFlowReactiveRawException() {
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
                                (message, mono) -> 
                                    mono.onErrorResume(
                                        WebClientResponseException.class,
                                        ex1 -> Mono.just(ex1)
                                                    .filter(ex -> ex.getStatusCode() == HttpStatus.NOT_FOUND)
                                                    .<Object>map(ex -> defaultPayload)
                                                    .switchIfEmpty(mono)
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
    public CommandLineRunner runner(MessagingTemplate messagingTemplate) {
        return (args) -> {
            Message<String> sourceMessage = MessageBuilder.withPayload("Two")
                                                            .build();
            
            System.out.println("WebClientResponseException.NotFound Implementation");
            try {
                final Message<?> resultMessage = 
                    messagingTemplate.sendAndReceive("getMixEntryFlowReactiveNotFound.input", sourceMessage);
                System.out.printf("Expecting resultMessage.headers.mix-entry-id == -1: %d%n", 
                                    resultMessage.getHeaders().get("mix-entry-id", Integer.class));
            } catch(MessageHandlingException ex) {
                System.out.println("ERROR - Trapped MessageHandlingException");
                try(FileWriter fw = new FileWriter("notfound-impl.txt");
                    PrintWriter pw = new PrintWriter(fw, true)) {
                    ex.printStackTrace(pw);
                }
            }
            System.out.println();

            System.out.println("WebClientResponseException (Raw) Implementation");
            try {
                final Message<?> resultMessage = 
                    messagingTemplate.sendAndReceive("getMixEntryFlowReactiveRawException.input", sourceMessage);
                System.out.printf("Expecting resultMessage.headers.mix-entry-id == -1: %d%n", 
                                    resultMessage.getHeaders().get("mix-entry-id", Integer.class));
            } catch(MessageHandlingException ex) {
                System.out.println("ERROR - Trapped MessageHandlingException");
                try(FileWriter fw = new FileWriter("raw-impl.txt");
                    PrintWriter pw = new PrintWriter(fw, true)) {
                    ex.printStackTrace(pw);
                }
            }
            System.out.println();
        };
    }
}
