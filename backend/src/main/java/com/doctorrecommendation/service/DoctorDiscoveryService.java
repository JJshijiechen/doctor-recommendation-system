package com.doctorrecommendation.service;

import com.doctorrecommendation.domain.Doctor;
import com.doctorrecommendation.domain.Specialty;
import com.doctorrecommendation.dto.RecommendationRequestDto;
import com.doctorrecommendation.dto.TermExtractionResult;
import com.doctorrecommendation.repository.DoctorRepository;
import com.doctorrecommendation.repository.SpecialtyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DoctorDiscoveryService {

    private static final int MAX_SPECIALTY_QUERIES = 2;
    private static final int PAGE_SIZE_PER_QUERY = 5;
    private static final int MAX_DOCTORS_TO_SAVE = 10;
    private static final String GOOGLE_FIELD_MASK = String.join(",",
            "places.id",
            "places.displayName",
            "places.formattedAddress",
            "places.location",
            "places.rating",
            "places.nationalPhoneNumber",
            "places.internationalPhoneNumber",
            "places.googleMapsUri",
            "places.types",
            "places.businessStatus"
    );
    private static final Pattern US_STATE_POSTAL = Pattern.compile("([A-Z]{2})\\s+([0-9A-Z-]{3,12}).*");

    private final String googlePlacesApiKey;
    private final String serpApiKey;
    private final RestClient googlePlacesClient;
    private final RestClient serpApiClient;
    private final ObjectMapper objectMapper;
    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final ProviderLookupCacheService providerLookupCacheService;

    public DoctorDiscoveryService(
            @Value("${app.google.places-api-key:}") String googlePlacesApiKey,
            @Value("${app.serpapi.api-key:}") String serpApiKey,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            DoctorRepository doctorRepository,
            SpecialtyRepository specialtyRepository,
            ProviderLookupCacheService providerLookupCacheService
    ) {
        this.googlePlacesApiKey = googlePlacesApiKey;
        this.serpApiKey = serpApiKey;
        this.googlePlacesClient = restClientBuilder.baseUrl("https://places.googleapis.com").build();
        this.serpApiClient = restClientBuilder.baseUrl("https://serpapi.com").build();
        this.objectMapper = objectMapper;
        this.doctorRepository = doctorRepository;
        this.specialtyRepository = specialtyRepository;
        this.providerLookupCacheService = providerLookupCacheService;
    }

    @Transactional
    public List<Doctor> discoverAndSave(RecommendationRequestDto request, TermExtractionResult extraction) {
        if (!hasCoordinates(request)) {
            return List.of();
        }

        List<DiscoveredDoctorProfile> profiles = List.of();
        if (hasText(googlePlacesApiKey)) {
            profiles = discoverWithGooglePlaces(request, extraction);
        }
        if (profiles.isEmpty() && hasText(serpApiKey)) {
            profiles = discoverWithSerpApi(request, extraction);
        }

        List<Doctor> savedDoctors = new ArrayList<>();
        for (DiscoveredDoctorProfile profile : limitByExternalId(profiles)) {
            savedDoctors.add(saveProfile(profile));
        }
        return savedDoctors;
    }

    private List<DiscoveredDoctorProfile> discoverWithGooglePlaces(RecommendationRequestDto request, TermExtractionResult extraction) {
        List<DiscoveredDoctorProfile> profiles = new ArrayList<>();
        for (String specialtyName : specialtyNames(extraction)) {
            String cacheKey = providerLookupCacheService.keyFor("google-places", request, specialtyName);
            Optional<List<DiscoveredDoctorProfile>> cachedProfiles = providerLookupCacheService.get(cacheKey);
            if (cachedProfiles.isPresent()) {
                profiles.addAll(cachedProfiles.get());
                continue;
            }

            try {
                List<DiscoveredDoctorProfile> fetchedProfiles = fetchGooglePlaces(request, specialtyName);
                providerLookupCacheService.put(cacheKey, fetchedProfiles);
                profiles.addAll(fetchedProfiles);
            } catch (Exception ignored) {
                return profiles;
            }
        }
        return profiles;
    }

    private List<DiscoveredDoctorProfile> fetchGooglePlaces(RecommendationRequestDto request, String specialtyName) throws Exception {
        List<DiscoveredDoctorProfile> profiles = new ArrayList<>();
        String response = googlePlacesClient.post()
                .uri("/v1/places:searchText")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Goog-Api-Key", googlePlacesApiKey)
                .header("X-Goog-FieldMask", GOOGLE_FIELD_MASK)
                .body(googleTextSearchBody(request, specialtyName))
                .retrieve()
                .body(String.class);
        JsonNode places = objectMapper.readTree(response).path("places");
        for (JsonNode place : places) {
            if ("CLOSED_PERMANENTLY".equals(place.path("businessStatus").asText())) {
                continue;
            }
            DiscoveredDoctorProfile profile = googleProfile(place, specialtyName);
            if (profile != null) {
                profiles.add(profile);
            }
        }
        return profiles;
    }

    private Map<String, Object> googleTextSearchBody(RecommendationRequestDto request, String specialtyName) {
        return Map.of(
                "textQuery", specialtyQuery(specialtyName),
                "pageSize", PAGE_SIZE_PER_QUERY,
                "locationBias", Map.of(
                        "circle", Map.of(
                                "center", Map.of(
                                        "latitude", request.latitude(),
                                        "longitude", request.longitude()
                                ),
                                "radius", radiusMeters(request)
                        )
                )
        );
    }

    private DiscoveredDoctorProfile googleProfile(JsonNode place, String specialtyName) {
        String externalId = text(place.path("id"));
        String displayName = text(place.path("displayName").path("text"));
        if (!hasText(externalId) || !hasText(displayName)) {
            return null;
        }
        Set<String> tags = new LinkedHashSet<>();
        tags.add(specialtyName);
        tags.add(specialtyQuery(specialtyName));
        tags.add("doctor");
        tags.add("google places");
        place.path("types").forEach(type -> tags.add(type.asText()));

        return new DiscoveredDoctorProfile(
                "google-places",
                externalId,
                text(place.path("googleMapsUri")),
                displayName,
                specialtyName,
                text(place.path("formattedAddress")),
                nullableDouble(place.path("location").path("latitude")),
                nullableDouble(place.path("location").path("longitude")),
                firstText(place.path("nationalPhoneNumber"), place.path("internationalPhoneNumber")),
                nullableDouble(place.path("rating")),
                tags
        );
    }

    private List<DiscoveredDoctorProfile> discoverWithSerpApi(RecommendationRequestDto request, TermExtractionResult extraction) {
        List<DiscoveredDoctorProfile> profiles = new ArrayList<>();
        for (String specialtyName : specialtyNames(extraction)) {
            String cacheKey = providerLookupCacheService.keyFor("serpapi-google-maps", request, specialtyName);
            Optional<List<DiscoveredDoctorProfile>> cachedProfiles = providerLookupCacheService.get(cacheKey);
            if (cachedProfiles.isPresent()) {
                profiles.addAll(cachedProfiles.get());
                continue;
            }

            try {
                List<DiscoveredDoctorProfile> fetchedProfiles = fetchSerpApi(request, specialtyName);
                providerLookupCacheService.put(cacheKey, fetchedProfiles);
                profiles.addAll(fetchedProfiles);
            } catch (Exception ignored) {
                return profiles;
            }
        }
        return profiles;
    }

    private List<DiscoveredDoctorProfile> fetchSerpApi(RecommendationRequestDto request, String specialtyName) throws Exception {
        List<DiscoveredDoctorProfile> profiles = new ArrayList<>();
        String ll = String.format(Locale.ROOT, "@%.6f,%.6f,%dm", request.latitude(), request.longitude(), (int) radiusMeters(request));
        String response = serpApiClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search.json")
                        .queryParam("engine", "google_maps")
                        .queryParam("type", "search")
                        .queryParam("q", specialtyQuery(specialtyName))
                        .queryParam("ll", ll)
                        .queryParam("nearby", "true")
                        .queryParam("hl", "en")
                        .queryParam("gl", "us")
                        .queryParam("api_key", serpApiKey)
                        .build())
                .retrieve()
                .body(String.class);
        JsonNode results = objectMapper.readTree(response).path("local_results");
        for (JsonNode result : results) {
            DiscoveredDoctorProfile profile = serpApiProfile(result, specialtyName);
            if (profile != null) {
                profiles.add(profile);
            }
        }
        return profiles;
    }

    private DiscoveredDoctorProfile serpApiProfile(JsonNode result, String specialtyName) {
        String displayName = text(result.path("title"));
        String externalId = firstText(result.path("place_id"), result.path("data_id"), result.path("data_cid"));
        if (!hasText(displayName) || !hasText(externalId)) {
            return null;
        }

        Set<String> tags = new LinkedHashSet<>();
        tags.add(specialtyName);
        tags.add(specialtyQuery(specialtyName));
        tags.add("doctor");
        tags.add("serpapi google maps");
        String type = text(result.path("type"));
        if (hasText(type)) {
            tags.add(type);
        }
        result.path("types").forEach(value -> tags.add(value.asText()));

        return new DiscoveredDoctorProfile(
                "serpapi-google-maps",
                externalId,
                googleMapsSearchUri(text(result.path("place_id")), displayName, text(result.path("address"))),
                displayName,
                specialtyName,
                text(result.path("address")),
                nullableDouble(result.path("gps_coordinates").path("latitude")),
                nullableDouble(result.path("gps_coordinates").path("longitude")),
                text(result.path("phone")),
                nullableDouble(result.path("rating")),
                tags
        );
    }

    private Doctor saveProfile(DiscoveredDoctorProfile profile) {
        Specialty specialty = specialtyRepository.findByNameIgnoreCase(profile.specialtyName())
                .orElseGet(() -> specialtyRepository.save(new Specialty(profile.specialtyName(), "Discovered provider specialty")));
        AddressParts address = parseAddress(profile.formattedAddress());
        Doctor doctor = doctorRepository.findByExternalProviderAndExternalId(profile.externalProvider(), profile.externalId())
                .orElseGet(Doctor::new);

        doctor.setFullName(profile.displayName());
        doctor.setSpecialty(specialty);
        doctor.setClinicName(profile.displayName());
        doctor.setBio("Live provider listing imported from %s for %s. Verify specialty, insurance, availability, and clinical fit directly with the provider before booking."
                .formatted(sourceLabel(profile.externalProvider()), profile.specialtyName()));
        doctor.setAddressLine(address.addressLine());
        doctor.setCity(address.city());
        doctor.setState(address.state());
        doctor.setPostalCode(address.postalCode());
        doctor.setLatitude(profile.latitude());
        doctor.setLongitude(profile.longitude());
        doctor.setPhone(profile.phone());
        doctor.setRating(profile.rating());
        doctor.setYearsExperience(null);
        doctor.setAcceptingNewPatients(true);
        doctor.setTelehealth(false);
        doctor.setNextAvailable("Call to confirm");
        doctor.setExternalProvider(profile.externalProvider());
        doctor.setExternalId(profile.externalId());
        doctor.setExternalUri(profile.externalUri());
        doctor.setLanguages(new LinkedHashSet<>());
        doctor.setAcceptedInsurances(new LinkedHashSet<>());
        doctor.setProfileTags(new LinkedHashSet<>(profile.tags()));
        return doctorRepository.save(doctor);
    }

    private List<DiscoveredDoctorProfile> limitByExternalId(List<DiscoveredDoctorProfile> profiles) {
        Map<String, DiscoveredDoctorProfile> unique = new LinkedHashMap<>();
        for (DiscoveredDoctorProfile profile : profiles) {
            unique.putIfAbsent(profile.externalProvider() + ":" + profile.externalId(), profile);
            if (unique.size() >= MAX_DOCTORS_TO_SAVE) {
                break;
            }
        }
        return new ArrayList<>(unique.values());
    }

    private List<String> specialtyNames(TermExtractionResult extraction) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        extraction.specialties().stream().filter(this::hasText).forEach(names::add);
        if (names.isEmpty()) {
            names.add("Family Medicine");
            names.add("Urgent Care");
        }
        return names.stream().limit(MAX_SPECIALTY_QUERIES).toList();
    }

    private String specialtyQuery(String specialtyName) {
        Map<String, String> queries = Map.ofEntries(
                Map.entry("family medicine", "family doctor"),
                Map.entry("internal medicine", "internal medicine doctor"),
                Map.entry("cardiology", "cardiologist"),
                Map.entry("pulmonology", "pulmonologist"),
                Map.entry("dermatology", "dermatologist"),
                Map.entry("neurology", "neurologist"),
                Map.entry("orthopedics", "orthopedic doctor"),
                Map.entry("gastroenterology", "gastroenterologist"),
                Map.entry("psychiatry", "psychiatrist"),
                Map.entry("obstetrics and gynecology", "obgyn doctor"),
                Map.entry("pediatrics", "pediatrician"),
                Map.entry("endocrinology", "endocrinologist"),
                Map.entry("allergy and immunology", "allergist"),
                Map.entry("urgent care", "urgent care doctor")
        );
        return queries.getOrDefault(normalize(specialtyName), specialtyName + " doctor");
    }

    private AddressParts parseAddress(String formattedAddress) {
        if (!hasText(formattedAddress)) {
            return new AddressParts("Address unavailable", "Nearby", "US", "00000");
        }

        String[] parts = formattedAddress.split(",");
        String addressLine = safePart(parts, 0, "Address unavailable");
        String city = safePart(parts, 1, "Nearby");
        String state = "US";
        String postalCode = "00000";
        if (parts.length > 2) {
            Matcher matcher = US_STATE_POSTAL.matcher(parts[2].trim());
            if (matcher.matches()) {
                state = matcher.group(1);
                postalCode = matcher.group(2);
            }
        }
        return new AddressParts(addressLine, city, state, postalCode);
    }

    private String safePart(String[] parts, int index, String fallback) {
        if (parts.length <= index || !hasText(parts[index])) {
            return fallback;
        }
        return parts[index].trim();
    }

    private boolean hasCoordinates(RecommendationRequestDto request) {
        return request.latitude() != null && request.longitude() != null;
    }

    private double radiusMeters(RecommendationRequestDto request) {
        double meters = request.effectiveRadiusMiles() * 1609.344;
        return Math.max(1000.0, Math.min(50000.0, meters));
    }

    private String googleMapsSearchUri(String placeId, String title, String address) {
        String query = encode(String.join(" ", List.of(nullToEmpty(title), nullToEmpty(address))).trim());
        if (hasText(placeId)) {
            return "https://www.google.com/maps/search/?api=1&query=%s&query_place_id=%s".formatted(query, encode(placeId));
        }
        return "https://www.google.com/maps/search/?api=1&query=%s".formatted(query);
    }

    private String sourceLabel(String externalProvider) {
        if ("google-places".equals(externalProvider)) {
            return "Google Places";
        }
        if ("serpapi-google-maps".equals(externalProvider)) {
            return "SerpAPI Google Maps";
        }
        return "external provider search";
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
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record AddressParts(String addressLine, String city, String state, String postalCode) {
    }
}
