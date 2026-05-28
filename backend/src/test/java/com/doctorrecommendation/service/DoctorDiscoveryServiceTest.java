package com.doctorrecommendation.service;

import com.doctorrecommendation.domain.Doctor;
import com.doctorrecommendation.domain.Specialty;
import com.doctorrecommendation.dto.RecommendationRequestDto;
import com.doctorrecommendation.dto.TermExtractionResult;
import com.doctorrecommendation.repository.DoctorRepository;
import com.doctorrecommendation.repository.SpecialtyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorDiscoveryServiceTest {

    @Test
    void usesCachedProviderLookupProfilesWithoutCallingExternalSearch() {
        DoctorRepository doctorRepository = mock(DoctorRepository.class);
        SpecialtyRepository specialtyRepository = mock(SpecialtyRepository.class);
        ProviderLookupCacheService providerLookupCache = mock(ProviderLookupCacheService.class);
        Specialty cardiology = new Specialty("Cardiology", "Heart care");
        RecommendationRequestDto request = new RecommendationRequestDto("chest pain", 41.8781, -87.6298, 20.0, "Aetna", "English", true);
        TermExtractionResult extraction = new TermExtractionResult(List.of("chest pain"), List.of("Cardiology"), "local-dictionary");
        DiscoveredDoctorProfile cachedProfile = new DiscoveredDoctorProfile(
                "serpapi-google-maps",
                "cached-place",
                "https://maps.example/cached-place",
                "Cached Cardiology Provider",
                "Cardiology",
                "100 Main St, Chicago, IL 60601",
                41.88,
                -87.63,
                "(312) 555-0100",
                4.9,
                Set.of("Cardiology", "serpapi google maps")
        );

        when(providerLookupCache.keyFor(eq("serpapi-google-maps"), eq(request), eq("Cardiology"))).thenReturn("provider-cache-key");
        when(providerLookupCache.get("provider-cache-key")).thenReturn(Optional.of(List.of(cachedProfile)));
        when(specialtyRepository.findByNameIgnoreCase("Cardiology")).thenReturn(Optional.of(cardiology));
        when(doctorRepository.findByExternalProviderAndExternalId("serpapi-google-maps", "cached-place")).thenReturn(Optional.empty());
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DoctorDiscoveryService service = new DoctorDiscoveryService(
                "",
                "serpapi-key",
                RestClient.builder(),
                new ObjectMapper().findAndRegisterModules(),
                doctorRepository,
                specialtyRepository,
                providerLookupCache
        );

        List<Doctor> savedDoctors = service.discoverAndSave(request, extraction);

        assertThat(savedDoctors).singleElement()
                .satisfies(doctor -> {
                    assertThat(doctor.getFullName()).isEqualTo("Cached Cardiology Provider");
                    assertThat(doctor.getPhone()).isEqualTo("(312) 555-0100");
                    assertThat(doctor.getExternalProvider()).isEqualTo("serpapi-google-maps");
                });
        verify(providerLookupCache, never()).put(eq("provider-cache-key"), any());
    }
}
