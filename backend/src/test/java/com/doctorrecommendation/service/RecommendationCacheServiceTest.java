package com.doctorrecommendation.service;

import com.doctorrecommendation.dto.RecommendationRequestDto;
import com.doctorrecommendation.dto.RecommendationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationCacheServiceTest {

    @Test
    void readsAndWritesRecommendationCache() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        RecommendationCacheService cache = new RecommendationCacheService(redis, mapper, 30);
        RecommendationRequestDto request = new RecommendationRequestDto("rash and itchy skin", 41.0, -87.0, 15.0, "Aetna", "English", false);
        RecommendationResponse response = new RecommendationResponse(1L, Instant.now(), false, "local-dictionary", List.of("rash"), List.of("Dermatology"), List.of(), "Not a diagnosis");
        String key = cache.keyFor(request);

        when(redis.opsForValue()).thenReturn(operations);
        when(operations.get(key)).thenReturn(mapper.writeValueAsString(response));

        assertThat(cache.get(key)).isPresent();

        cache.put(key, response);

        verify(operations).set(eq(key), any(String.class), any(Duration.class));
    }
}
