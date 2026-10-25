package com.allobank.splitbill.service;

import org.springframework.stereotype.Component;

@Component
public class ServiceChargeCalculator {

    public int calculatePercentage(String githubUsername) {
        if (githubUsername == null || githubUsername.isBlank()) {
            throw new IllegalArgumentException(
                "GitHub username must not be blank"
            );
        }

        return githubUsername.toLowerCase()
            .chars()
            .sum() % 10;
    }
}
