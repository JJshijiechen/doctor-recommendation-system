package com.doctorrecommendation.service;

import com.doctorrecommendation.domain.Doctor;
import com.doctorrecommendation.domain.Specialty;
import com.doctorrecommendation.dto.RecommendationRequestDto;
import com.doctorrecommendation.dto.TermExtractionResult;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TfIdfRankingServiceTest {

    private final TfIdfRankingService rankingService = new TfIdfRankingService();

    @Test
    void ranksCardiologyDoctorFirstForChestPainAndShortnessOfBreath() {
        Doctor cardiologist = doctor("Northwestern Memorial Hospital Cardiology", "Cardiology", "chest pain shortness of breath heart palpitations", 41.8946, -87.6215);
        Doctor dermatologist = doctor("Chicago Dermatology Provider", "Dermatology", "rash acne eczema itchy skin", 41.8941, -87.6224);

        RecommendationRequestDto request = new RecommendationRequestDto(
                "chest pain and shortness of breath near Chicago",
                41.8781,
                -87.6298,
                20.0,
                "Aetna",
                "English",
                true
        );
        TermExtractionResult extraction = new TermExtractionResult(
                List.of("chest pain", "shortness of breath"),
                List.of("Cardiology", "Pulmonology"),
                "local-dictionary"
        );

        List<RankedDoctor> ranked = rankingService.rank(List.of(dermatologist, cardiologist), extraction, request);

        assertThat(ranked).first().extracting(result -> result.doctor().getSpecialty().getName()).isEqualTo("Cardiology");
        assertThat(ranked.getFirst().score()).isGreaterThan(ranked.get(1).score());
    }

    private Doctor doctor(String name, String specialtyName, String tags, Double latitude, Double longitude) {
        Doctor doctor = new Doctor();
        doctor.setFullName(name);
        doctor.setSpecialty(new Specialty(specialtyName, specialtyName + " care"));
        doctor.setClinicName(name + " Clinic");
        doctor.setBio(tags);
        doctor.setAddressLine("100 Main St");
        doctor.setCity("Chicago");
        doctor.setState("IL");
        doctor.setPostalCode("60611");
        doctor.setLatitude(latitude);
        doctor.setLongitude(longitude);
        doctor.setRating(4.8);
        doctor.setAcceptingNewPatients(true);
        doctor.setTelehealth(true);
        doctor.setLanguages(new LinkedHashSet<>(Set.of("English")));
        doctor.setAcceptedInsurances(new LinkedHashSet<>(Set.of("Aetna")));
        doctor.setProfileTags(new LinkedHashSet<>(List.of(tags.split(" "))));
        return doctor;
    }
}
