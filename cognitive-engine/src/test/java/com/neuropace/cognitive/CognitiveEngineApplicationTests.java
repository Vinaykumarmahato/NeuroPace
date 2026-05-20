package com.neuropace.cognitive;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CognitiveEngineApplicationTests {

    private static final Logger log = LoggerFactory.getLogger(CognitiveEngineApplicationTests.class);

    private static MySQLContainer<?> mysql;
    private static GenericContainer<?> redis;
    private static boolean useTestcontainers = false;

    static {
        try {
            log.info("Attempting to initialize Testcontainers for MySQL and Redis...");
            mysql = new MySQLContainer<>("mysql:8")
                    .withDatabaseName("neuropace")
                    .withUsername("root")
                    .withPassword("password");
            
            redis = new GenericContainer<>("redis:7")
                    .withExposedPorts(6379);

            mysql.start();
            redis.start();
            useTestcontainers = true;
            log.info("Testcontainers successfully started.");
        } catch (Exception e) {
            log.warn("Failed to initialize Testcontainers. Falling back to local docker-compose environment. Error: {}", e.getMessage());
            if (mysql != null && mysql.isRunning()) {
                try { mysql.stop(); } catch (Exception ignored) {}
            }
            if (redis != null && redis.isRunning()) {
                try { redis.stop(); } catch (Exception ignored) {}
            }
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (useTestcontainers) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl);
            registry.add("spring.datasource.username", mysql::getUsername);
            registry.add("spring.datasource.password", mysql::getPassword);
            registry.add("spring.data.redis.host", redis::getHost);
            registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        } else {
            log.info("Configuring dynamic properties to use local docker-compose services (localhost:3307, localhost:6379)...");
            registry.add("spring.datasource.url", () -> "jdbc:mysql://localhost:3307/neuropace");
            registry.add("spring.datasource.username", () -> "root");
            registry.add("spring.datasource.password", () -> "password");
            registry.add("spring.data.redis.host", () -> "localhost");
            registry.add("spring.data.redis.port", () -> 6379);
        }
    }

    @Test
    void contextLoads() {
        log.info("Spring Boot Context loaded successfully.");
        if (useTestcontainers) {
            assertThat(mysql.isRunning()).isTrue();
            assertThat(redis.isRunning()).isTrue();
        }
    }
}
