package com.doctorrecommendation.service;

import com.doctorrecommendation.dto.RecommendationRequestDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ProviderLookupCacheService {

    private static final TypeReference<List<DiscoveredDoctorProfile>> PROFILE_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final Counter hitCounter;
    private final Counter missCounter;
    private final Counter errorCounter;
    private final Counter writeCounter;

    public ProviderLookupCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${app.cache.provider-lookup-ttl-minutes:1440}") long ttlMinutes
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofMinutes(ttlMinutes);
        this.hitCounter = Counter.builder("doctor.provider.lookup.cache.hits")
                .description("Redis cache hits for external provider lookups")
                .register(meterRegistry);
        this.missCounter = Counter.builder("doctor.provider.lookup.cache.misses")
                .description("Redis cache misses for external provider lookups")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("doctor.provider.lookup.cache.errors")
                .description("Redis cache read/write errors for external provider lookups")
                .register(meterRegistry);
        this.writeCounter = Counter.builder("doctor.provider.lookup.cache.writes")
                .description("Redis cache writes for external provider lookups")
                .register(meterRegistry);
    }

    public String keyFor(String externalProvider, RecommendationRequestDto request, String specialtyName) {
        String normalized = String.join("|",
                "v1",
                normalize(externalProvider),
                normalize(specialtyName),
                roundedCoordinate(request.latitude()),
                roundedCoordinate(request.longitude()),
                roundedRadius(request.effectiveRadiusMiles())
        );
        return "provider-lookup:" + sha256(normalized);
    }

    public Optional<List<DiscoveredDoctorProfile>> get(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                missCounter.increment();
                return Optional.empty();
            }
            hitCounter.increment();
            return Optional.of(objectMapper.readValue(value, PROFILE_LIST_TYPE));
        } catch (Exception exception) {
            errorCounter.increment();
            return Optional.empty();
        }
    }

    public void put(String key, List<DiscoveredDoctorProfile> profiles) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(profiles), ttl);
            writeCounter.increment();
        } catch (Exception ignored) {
            errorCounter.increment();
            // External lookup caching must not block patient-facing recommendations.
        }
    }

    private String roundedCoordinate(Double value) {
        return value == null ? "" : String.format(Locale.ROOT, "%.4f", value);
    }

    private String roundedRadius(Double value) {
        return value == null ? "" : String.format(Locale.ROOT, "%.1f", value);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash provider lookup cache key", exception);
        }
    }
}
