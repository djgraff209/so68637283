package com.oneangrybean.proto.mixintegration;

import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.web.client.HttpClientErrorException;

public class NotFoundRequestHandlerAdvice extends AbstractRequestHandlerAdvice {
    @Override
    protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
        Object result;
        try {
            result = callback.execute();
        }
        catch(ThrowableHolderException ex) {
            final MessageHandlingException exceptionCause = (MessageHandlingException)unwrapExceptionIfNecessary(ex);
            if( exceptionCause.getCause() instanceof HttpClientErrorException.NotFound ) {
                result =  "{ \"id\": -1, \"name\": \"NOT FOUND\" }";
            }
            else {
                throw ex;
            }
        }
        return result;
    }
}