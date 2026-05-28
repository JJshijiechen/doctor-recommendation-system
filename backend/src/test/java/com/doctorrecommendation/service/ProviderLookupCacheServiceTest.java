package com.doctorrecommendation.service;

import com.doctorrecommendation.dto.RecommendationRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderLookupCacheServiceTest {

    @Test
    void readsAndWritesProviderLookupCache() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ProviderLookupCacheService cache = new ProviderLookupCacheService(redis, mapper, registry, 60);
        RecommendationRequestDto request = new RecommendationRequestDto("chest pain", 41.87812, -87.62981, 20.0, "Aetna", "English", true);
        DiscoveredDoctorProfile profile = new DiscoveredDoctorProfile(
                "serpapi-google-maps",
                "place-1",
                "https://maps.example/place-1",
                "Loop Cardiology",
                "Cardiology",
                "100 Main St, Chicago, IL 60601",
                41.88,
                -87.63,
                "(312) 555-0199",
                4.8,
                Set.of("Cardiology", "serpapi google maps")
        );
        String key = cache.keyFor("serpapi-google-maps", request, "Cardiology");

        when(redis.opsForValue()).thenReturn(operations);
        when(operations.get(key)).thenReturn(mapper.writeValueAsString(List.of(profile)));

        Optional<List<DiscoveredDoctorProfile>> cachedProfiles = cache.get(key);

        assertThat(cachedProfiles).isPresent();
        assertThat(cachedProfiles.get()).singleElement().extracting(DiscoveredDoctorProfile::phone).isEqualTo("(312) 555-0199");
        assertThat(registry.get("doctor.provider.lookup.cache.hits").counter().count()).isEqualTo(1.0);

        cache.put(key, List.of(profile));

        verify(operations).set(eq(key), any(String.class), eq(Duration.ofMinutes(60)));
        assertThat(registry.get("doctor.provider.lookup.cache.writes").counter().count()).isEqualTo(1.0);
    }

    @Test
    void roundsCoordinatesForStableLookupKeys() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        ProviderLookupCacheService cache = new ProviderLookupCacheService(redis, mapper, new SimpleMeterRegistry(), 60);
        RecommendationRequestDto first = new RecommendationRequestDto("chest pain", 41.87811, -87.62981, 20.0, null, null, false);
        RecommendationRequestDto second = new RecommendationRequestDto("different filters", 41.87812, -87.62982, 20.0, "Cigna", "Spanish", true);

        assertThat(cache.keyFor("serpapi-google-maps", first, "Cardiology"))
                .isEqualTo(cache.keyFor("serpapi-google-maps", second, "Cardiology"));
    }
}
