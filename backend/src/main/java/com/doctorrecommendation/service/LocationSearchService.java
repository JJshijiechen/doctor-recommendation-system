package com.doctorrecommendation.service;

import com.doctorrecommendation.dto.LocationSearchResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LocationSearchService {

    private static final int MAX_RESULTS = 5;
    private static final String GOOGLE_LOCATION_FIELD_MASK = String.join(",",
            "places.id",
            "places.displayName",
            "places.formattedAddress",
            "places.location"
    );
    private static final Pattern COORDINATE_PAIR = Pattern.compile(
            "^\\s*(-?\\d{1,2}(?:\\.\\d+)?)\\s*,\\s*(-?\\d{1,3}(?:\\.\\d+)?)\\s*$"
    );
    private static final TypeReference<List<LocationSearchResponse>> LOCATION_LIST_TYPE = new TypeReference<>() {
    };

    private final String googlePlacesApiKey;
    private final String serpApiKey;
    private final RestClient googlePlacesClient;
    private final RestClient serpApiClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public LocationSearchService(
            @Value("${app.google.places-api-key:}") String googlePlacesApiKey,
            @Value("${app.serpapi.api-key:}") String serpApiKey,
            @Value("${app.cache.location-search-ttl-minutes:1440}") long ttlMinutes,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate
    ) {
        this.googlePlacesApiKey = googlePlacesApiKey;
        this.serpApiKey = serpApiKey;
        this.googlePlacesClient = restClientBuilder.baseUrl("https://places.googleapis.com").build();
        this.serpApiClient = restClientBuilder.baseUrl("https://serpapi.com").build();
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public List<LocationSearchResponse> search(String rawQuery) {
        String query = normalize(rawQuery);
        if (!hasText(query)) {
            return List.of();
        }

        Optional<LocationSearchResponse> coordinateResult = parseCoordinatePair(query);
        if (coordinateResult.isPresent()) {
            return List.of(coordinateResult.get());
        }

        String cacheKey = "location-search:" + sha256(query);
        Optional<List<LocationSearchResponse>> cached = readCache(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        List<LocationSearchResponse> results = List.of();
        if (hasText(googlePlacesApiKey)) {
            results = searchGooglePlaces(query);
        }
        if (results.isEmpty() && hasText(serpApiKey)) {
            results = searchSerpApi(query);
        }
        writeCache(cacheKey, results);
        return results;
    }

    private List<LocationSearchResponse> searchGooglePlaces(String query) {
        try {
            String response = googlePlacesClient.post()
                    .uri("/v1/places:searchText")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Goog-Api-Key", googlePlacesApiKey)
                    .header("X-Goog-FieldMask", GOOGLE_LOCATION_FIELD_MASK)
                    .body(Map.of(
                            "textQuery", query,
                            "pageSize", MAX_RESULTS,
                            "languageCode", "en"
                    ))
                    .retrieve()
                    .body(String.class);
            List<LocationSearchResponse> locations = new ArrayList<>();
            objectMapper.readTree(response).path("places").forEach(place -> {
                Double latitude = nullableDouble(place.path("location").path("latitude"));
                Double longitude = nullableDouble(place.path("location").path("longitude"));
                String label = firstText(place.path("displayName").path("text"), place.path("formattedAddress"));
                if (hasText(label) && latitude != null && longitude != null) {
                    locations.add(new LocationSearchResponse(
                            label,
                            text(place.path("formattedAddress")),
                            latitude,
                            longitude,
                            "google-places"
                    ));
                }
            });
            return uniqueLocations(locations);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<LocationSearchResponse> searchSerpApi(String query) {
        try {
            String response = serpApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search.json")
                            .queryParam("engine", "google_maps")
                            .queryParam("type", "search")
                            .queryParam("q", query)
                            .queryParam("hl", "en")
                            .queryParam("gl", "us")
                            .queryParam("api_key", serpApiKey)
                            .build())
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            List<LocationSearchResponse> locations = new ArrayList<>();
            addSerpApiResult(locations, root.path("place_results"));
            root.path("local_results").forEach(result -> addSerpApiResult(locations, result));
            return uniqueLocations(locations);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private void addSerpApiResult(List<LocationSearchResponse> locations, JsonNode result) {
        if (result == null || result.isMissingNode() || result.isNull()) {
            return;
        }
        Double latitude = nullableDouble(result.path("gps_coordinates").path("latitude"));
        Double longitude = nullableDouble(result.path("gps_coordinates").path("longitude"));
        String label = firstText(result.path("title"), result.path("name"), result.path("address"));
        if (!hasText(label) || latitude == null || longitude == null) {
            return;
        }
        locations.add(new LocationSearchResponse(
                label,
                text(result.path("address")),
                latitude,
                longitude,
                "serpapi-google-maps"
        ));
    }

    private Optional<LocationSearchResponse> parseCoordinatePair(String query) {
        Matcher matcher = COORDINATE_PAIR.matcher(query);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        double latitude = Double.parseDouble(matcher.group(1));
        double longitude = Double.parseDouble(matcher.group(2));
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            return Optional.empty();
        }
        return Optional.of(new LocationSearchResponse(
                "Entered coordinates",
                "%.4f, %.4f".formatted(latitude, longitude),
                latitude,
                longitude,
                "manual-coordinates"
        ));
    }

    private List<LocationSearchResponse> uniqueLocations(List<LocationSearchResponse> locations) {
        Map<String, LocationSearchResponse> unique = new LinkedHashMap<>();
        for (LocationSearchResponse location : locations) {
            String key = String.format(Locale.ROOT, "%.4f|%.4f|%s",
                    location.latitude(),
                    location.longitude(),
                    normalize(location.label()));
            unique.putIfAbsent(key, location);
            if (unique.size() >= MAX_RESULTS) {
                break;
            }
        }
        return new ArrayList<>(unique.values());
    }

    private Optional<List<LocationSearchResponse>> readCache(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (!hasText(value)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, LOCATION_LIST_TYPE));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private void writeCache(String key, List<LocationSearchResponse> results) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(results), ttl);
        } catch (Exception ignored) {
            // Location lookup caching must not block user input.
        }
    }

    private String firstText(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            String value = text(node);
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText(null);
    }

    private Double nullableDouble(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() || !node.isNumber() ? null : node.asDouble();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash location search cache key", exception);
        }
    }
}
