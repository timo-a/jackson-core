package com.fasterxml.jackson.core.read.loc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MyTest {

    @Test
    public void test() {
        assertThrows(IllegalStateException.class, () -> { while (System.out != null) {} });
    }
}
