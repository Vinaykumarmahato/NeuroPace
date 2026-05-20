package com.neuropace.curriculum;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    public record HealthStatus(String status) {}

    @GetMapping("/actuator/health")
    public HealthStatus health() {
        return new HealthStatus("UP");
    }
}
