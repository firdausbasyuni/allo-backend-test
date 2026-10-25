package com.allobank.splitbill.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceChargeCalculatorTest {

    private final ServiceChargeCalculator calculator =
        new ServiceChargeCalculator();

    @Test
    void calculatesThreePercentForGithubUsername() {
        int percentage = calculator.calculatePercentage("firdausbasyuni");

        assertEquals(3, percentage);
    }

    @Test
    void calculationIsCaseInsensitive() {
        assertEquals(
            calculator.calculatePercentage("firdausbasyuni"),
            calculator.calculatePercentage("FirdausBasyuni")
        );
    }

    @Test
    void rejectsBlankUsername() {
        assertThrows(
            IllegalArgumentException.class,
            () -> calculator.calculatePercentage(" ")
        );
    }
}
