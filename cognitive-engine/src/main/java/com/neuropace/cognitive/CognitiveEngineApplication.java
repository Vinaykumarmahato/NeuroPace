package com.neuropace.cognitive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CognitiveEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(CognitiveEngineApplication.class, args);
    }
}
