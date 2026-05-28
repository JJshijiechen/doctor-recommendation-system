package com.doctorrecommendation.controller;

import com.doctorrecommendation.dto.RecommendationRequestDto;
import com.doctorrecommendation.dto.RecommendationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecommendationIntegrationIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("doctor_recommendation")
            .withUsername("doctor")
            .withPassword("doctor");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("app.security.enabled", () -> "false");
        registry.add("app.openai.api-key", () -> "");
        registry.add("app.google.places-api-key", () -> "");
        registry.add("app.serpapi.api-key", () -> "");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void returnsNoDoctorsWhenNoExternalKeysOrAdminDoctors() {
        RecommendationRequestDto request = new RecommendationRequestDto(
                "chest pain and shortness of breath near Chicago",
                41.8781,
                -87.6298,
                20.0,
                "Aetna",
                "English",
                true
        );

        RecommendationResponse response = restTemplate.postForObject("/api/recommendations", request, RecommendationResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.extractionSource()).isEqualTo("local-dictionary");
        assertThat(response.predictedSpecialties()).contains("Cardiology");
        assertThat(response.results()).isEmpty();
    }
}
