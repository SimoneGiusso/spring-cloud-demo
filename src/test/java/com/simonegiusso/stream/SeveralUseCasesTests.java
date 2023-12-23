package com.simonegiusso.stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Import({TestChannelBinderConfiguration.class})
public class SeveralUseCasesTests {

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private OutputDestination outputDestination;

    @Test
    void testSingleFunction() {
        Message<byte[]> inputMessage = MessageBuilder.withPayload("Hello".getBytes()).build();
        inputDestination.send(inputMessage, "uppercase-in-0");

        Message<byte[]> outputMessage = outputDestination.receive(0, "uppercase-out-0");
        assertThat(outputMessage.getPayload()).isEqualTo("HELLO".getBytes());
    }

    @Test
    void testInterceptor() {
        Message<byte[]> inputMessage = MessageBuilder.withPayload("Hello".getBytes()).build();
        inputDestination.send(inputMessage, "reverse-in-0");

        Message<byte[]> outputMessage = outputDestination.receive(0, "reverse-out-0");
        assertThat(outputMessage.getPayload()).isEqualTo("olleH".getBytes());
        assertThat(outputMessage.getHeaders()
            .get("Interceptor-Header")).isNotNull(); // Added by interceptor before sending the message
    }

    @Test
    void testCompositeFunction() {
        Message<byte[]> inputMessage = MessageBuilder.withPayload("Hello".getBytes()).build();
        inputDestination.send(inputMessage, "uppercasereverse-in-0");

        Message<byte[]> outputMessage = outputDestination.receive(0, "uppercasereverse-out-0");
        assertThat(outputMessage.getPayload()).isEqualTo("OLLEH".getBytes());
    }
}