# Overview

This code base is to support a question to [Stack Overflow](https://stackoverflow.com/questions/68637283).

Additionally this is to demonstrate a problem discovered in Spring Integration [#3610](https://github.com/spring-projects/spring-integration/issues/3610)

## Operation

This project contains two applications:
* mix-repo
* mix-integration (depends on mix-repo running)

Start `mix-repo` in one terminal window: `./gradlew :mix-repo:bootRun`

The application will start and serve on TCP port 8080 on the localhost.

Start `mix-integration` in another terminal window: `./gradlew :mix-integration:bootRun`

Observe the output.

```
WebClientResponseException.NotFound Implementation
ERROR - Trapped MessageHandlingException

WebClientResponseException (Raw) Implementation
2021-08-06 17:09:53.217  INFO 52140 --- [ctor-http-nio-2] o.s.integration.handler.LoggingHandler   : GenericMessage [payload={ "id": 
-1, "name": null }, headers={replyChannel=org.springframework.messaging.core.GenericMessagingTemplate$TemporaryReplyChannel@1be07998, errorChannel=org.springframework.messaging.core.GenericMessagingTemplate$TemporaryReplyChannel@1be07998, id=9dcff546-7563-d1a2-ba9e-05bedfe8f526, timestamp=1628284193217}]
2021-08-06 17:09:53.314  INFO 52140 --- [  restartedMain] o.s.integration.handler.LoggingHandler   : GenericMessage [payload=Two, headers={replyChannel=org.springframework.messaging.core.GenericMessagingTemplate$TemporaryReplyChannel@533301da, errorChannel=org.springframework.messaging.core.GenericMessagingTemplate$TemporaryReplyChannel@533301da, id=e475ed6b-8491-4e76-5fd2-75f3e17e3b71, mix-entry-id=-1, timestamp=1628284193313}]
Expecting resultMessage.headers.mix-entry-id == -1: -1
```

Note the line near `WebClientResponseException.NotFound Implementation` that an error is logged indicating a `MessageHandlingException`. This should not occur if the implementation utilizes the
`WebClientResponseException.create()` method.
