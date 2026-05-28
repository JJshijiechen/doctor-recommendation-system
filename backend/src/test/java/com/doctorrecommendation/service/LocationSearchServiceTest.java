package com.doctorrecommendation.service;

import com.doctorrecommendation.dto.LocationSearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocationSearchServiceTest {

    @Test
    void parsesCoordinatePairWithoutExternalLookup() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        LocationSearchService service = new LocationSearchService(
                "",
                "",
                60,
                RestClient.builder(),
                new ObjectMapper().findAndRegisterModules(),
                redis
        );

        List<LocationSearchResponse> results = service.search("42.3371, -71.1056");

        assertThat(results).singleElement()
                .satisfies(location -> {
                    assertThat(location.label()).isEqualTo("Entered coordinates");
                    assertThat(location.latitude()).isEqualTo(42.3371);
                    assertThat(location.longitude()).isEqualTo(-71.1056);
                    assertThat(location.source()).isEqualTo("manual-coordinates");
                });
    }

    @Test
    void returnsCachedNaturalLanguageLocationBeforeExternalLookup() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        LocationSearchService service = new LocationSearchService(
                "",
                "serpapi-key",
                60,
                RestClient.builder(),
                mapper,
                redis
        );
        List<LocationSearchResponse> cached = List.of(
                new LocationSearchResponse("Boston Children's Hospital", "300 Longwood Ave, Boston, MA", 42.3371, -71.1056, "serpapi-google-maps")
        );

        when(redis.opsForValue()).thenReturn(operations);
        when(operations.get(anyString())).thenReturn(mapper.writeValueAsString(cached));

        List<LocationSearchResponse> results = service.search("Boston Children's Hospital");

        assertThat(results).singleElement()
                .extracting(LocationSearchResponse::address)
                .isEqualTo("300 Longwood Ave, Boston, MA");
    }
}
