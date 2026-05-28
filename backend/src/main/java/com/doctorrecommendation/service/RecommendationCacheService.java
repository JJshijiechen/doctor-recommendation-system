package com.doctorrecommendation.service;

import com.doctorrecommendation.dto.RecommendationRequestDto;
import com.doctorrecommendation.dto.RecommendationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class RecommendationCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RecommendationCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.cache.recommendation-ttl-minutes:30}") long ttlMinutes
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public String keyFor(RecommendationRequestDto request) {
        String normalized = String.join("|",
                normalize(request.symptoms()),
                String.valueOf(request.latitude()),
                String.valueOf(request.longitude()),
                String.valueOf(request.effectiveRadiusMiles()),
                normalize(request.insurance()),
                normalize(request.language()),
                String.valueOf(Boolean.TRUE.equals(request.telehealthPreferred()))
        );
        return "doctor-recommendation:" + sha256(normalized);
    }

    public Optional<RecommendationResponse> get(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, RecommendationResponse.class));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public void put(String key, RecommendationResponse response) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(response), ttl);
        } catch (Exception ignored) {
            // Cache availability must not block patient-facing recommendations.
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash recommendation cache key", exception);
        }
    }
}
