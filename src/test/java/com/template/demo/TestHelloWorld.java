package com.template.demo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestHelloWorld {

    @Test
    public void testHelloWorld() {
        DemoHelloWorld demoHelloWorld = new DemoHelloWorld();
        assertEquals(demoHelloWorld.helloWorld(), "Hello, World!");
    }
}
