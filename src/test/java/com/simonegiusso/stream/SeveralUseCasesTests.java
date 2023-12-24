package com.simonegiusso.stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Import({TestChannelBinderConfiguration.class})
public class SeveralUseCasesTests {

    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    @Autowired
    private InputDestination inputDestination;
    @Autowired
    private OutputDestination outputDestination;

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(standardOut);
    }

    /**
     * The naming convention used to name input and output bindings is as follows:
     * <ul>
     *     <li> input - &lt;functionName&gt; + -in- + &lt;index&gt; </li>
     *     <li> output - &lt;functionName&gt; + -out- + &lt;index&gt; </li>
     * </ul>
     * So if for example you would want to map the input of this function to a remote destination (e.g., topic, queue etc) called "my-topic" you would do so with the following property:
     * <p>
     * &emsp; {@code spring.cloud.stream.bindings.uppercase-in-0.destination=my-topic}
     */
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

    /**
     * It is possible to process and/or product multiple streams
     */
    @Test
    void testDoubleInputSingleOutput() {
        Message<byte[]> inputMessage0 = MessageBuilder.withPayload("Hello".getBytes()).build();
        Message<byte[]> inputMessage1 = MessageBuilder.withPayload("0".getBytes()).build();
        inputDestination.send(inputMessage0, "gather-in-0");
        inputDestination.send(inputMessage1, "gather-in-1");

        Message<byte[]> outputMessage = outputDestination.receive(0, "gather-out-0");
        assertThat(outputMessage.getPayload()).isEqualTo("Hello0".getBytes());
    }

    @Test
    void testBatchConsumer() {
        List<String> messages = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            messages.add(String.valueOf(i));
        }

        Message<List<String>> inputMessage = MessageBuilder.withPayload(messages).build();
        inputDestination.send(inputMessage, "concat-in-0");

        Message<byte[]> outputMessage = outputDestination.receive(0, "concat-out-0");
        assertThat(outputMessage.getPayload()).isEqualTo("01234".getBytes());
    }

    @Test
    void testPostProcessingFunction() {
        Message<byte[]> inputMessage = MessageBuilder.withPayload("Hello".getBytes()).build();
        inputDestination.send(inputMessage, "uppercasePostProcessingFunction-in-0");

        Message<byte[]> outputMessage = outputDestination.receive(0, "uppercasePostProcessingFunction-out-0");
        assertThat(outputMessage.getPayload()).isEqualTo("HELLO".getBytes());
        assertThat("Post processing...").isEqualTo(outputStreamCaptor.toString().trim());
    }

    /**
     * It is possible to handle errors with a custom function. By default the framework log the message and drop it. <p>
     * Retry are performed by the framework (by default 3). This can be changed with:
     * <p>
     * &emsp; {@code --spring.cloud.stream.bindings.uppercase-in-0.consumer.max-attempts=1}
     * <p>
     * The retry mechanism is futher customizable as explained <a href="https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/overview-error-handling.html#retry-template">here</a>.
     * <p>
     * Also it is possible to active DLQ where failed messages are sent to a special destination/queue. See more <a href="https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/overview-error-handling.html#dlq-dead-letter-queue">here</a>
     */
    @Test
    void testCustomErrorHandler() {
        Message<byte[]> inputMessage = MessageBuilder.withPayload("Hello".getBytes()).build();
        inputDestination.send(inputMessage, "functionWithError-in-0");

        assertThat(outputStreamCaptor.toString().trim()).contains("Error detected!");
    }
}