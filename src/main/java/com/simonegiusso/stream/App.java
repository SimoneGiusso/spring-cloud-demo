package com.simonegiusso.stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.util.Date;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SpringBootApplication
public class App {

    @Autowired
    private StreamBridge streamBridge;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
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
    @Bean
    public Function<String, String> uppercase() {
        return String::toUpperCase;
    }

    /**
     * Just consume messages
     */
    @Bean
    public Consumer<String> sink() {
        return System.out::println;
    }

    /**
     * While Consumer and Function are triggered based on data (events) sent to the destination they are bound to.
     * The Supplier bean produces whenever its get() method is invoked. <p>
     * The framework provides a default invocation of the supplier every second. See how to change it <a href="https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/producing-and-consuming-messages.html#polling-configuration-properties">here</a>.
     */
    @Bean
    public Supplier<Date> date() {
        return () -> new Date(12345L);
    }

    @Bean
    public Function<String, String> reverse() {
        return message -> new StringBuilder(message).reverse().toString();
    }

    /**
     * The framework recognizes the difference in the programming style and guarantees that such a supplier
     * is triggered only once since it already returns an infinitive stream.
     * <p>
     * In case the stream is finite and then the supplier must be invoked more than once the {@code @PollableBean} annotation
     * can be used instead of {@code @Bean} to tell the framework that, even if it's reactive, it has to be called
     * more than once.
     * <p>
     * In the event you are using regular Kafka or Rabbit or any other non-reactive binder, you can only benefit
     * from the conveniences of the reactive API itself and not its advanced features,
     * since the actual sources or targets of the stream are not reactive.
     */
    @Bean
    public Supplier<Flux<String>> stringSupplier() {
        return () -> Flux.fromStream(Stream.generate(() -> { // Returns an infinite sequential unordered stream
            try {
                Thread.sleep(1000);
                return "Hello from Supplier";
            } catch (Exception e) {
                // ignore
            }
            return null;
        })).subscribeOn(Schedulers.boundedElastic()).share();
    }

    /**
     * {@code StreamBridge} allows us to send data to an output binding effectively bridging non-stream application
     * with spring-cloud-stream.
     */
    @RequestMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void delegateToSupplier(@RequestBody String body) {
        System.out.println("Sending " + body);
        streamBridge.send("toStream", body); // it will also initiate creation of output bindings on the first call if it doesn't exist. By default, maximum dynamic binders are set to 10 to avoid memory leaks. It means that after 10 a binding will be deleted and then recreated with then a minor performance degradation.
    }

    /**
     * It is possible to process and/or product multiple streams
     */
    @Bean
    public Function<Tuple2<Flux<String>, Flux<Integer>>, Flux<String>> gather() {
        return tuple -> { // the two input bindings will be gather-in-0 and gather-in-1
            Flux<String> stringStream = tuple.getT1();
            Flux<String> intStream = tuple.getT2().map(String::valueOf);
            return Flux.merge(stringStream, intStream);
        };
    }

}