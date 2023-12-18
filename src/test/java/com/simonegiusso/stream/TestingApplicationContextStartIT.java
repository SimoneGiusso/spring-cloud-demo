package com.simonegiusso.stream;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * A simple sanity check test that will fail if the application context cannot start.
 */
@SpringBootTest // Tells Spring Boot to look for a main configuration class (one with @SpringBootApplication, for instance) and use that to start a Spring application context.
public class TestingApplicationContextStartIT {

    @Test
    public void contextLoads() {}

}