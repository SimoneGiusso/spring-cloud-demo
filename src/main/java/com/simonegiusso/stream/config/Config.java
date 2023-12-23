package com.simonegiusso.stream.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;

@Configuration
public class Config {

    @Bean
    @GlobalChannelInterceptor(patterns = "reverse-out-0")
    public ChannelInterceptor interceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                return MessageBuilder
                    .withPayload(message.getPayload())
                    .copyHeaders(message.getHeaders())
                    .setHeader("Interceptor-Header", "Value")
                    .build();
            }
        };
    }

}