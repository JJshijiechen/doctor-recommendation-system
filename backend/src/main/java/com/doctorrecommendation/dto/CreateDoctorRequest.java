package com.doctorrecommendation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateDoctorRequest(
        @NotBlank @Size(max = 140) String fullName,
        @NotBlank @Size(max = 80) String specialty,
        @NotBlank @Size(max = 140) String clinicName,
        @Size(max = 2000) String bio,
        @NotBlank @Size(max = 160) String addressLine,
        @NotBlank @Size(max = 80) String city,
        @NotBlank @Size(max = 2) String state,
        @NotBlank @Size(max = 16) String postalCode,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Size(max = 40) String phone,
        @DecimalMin("0.0") @DecimalMax("5.0") Double rating,
        Integer yearsExperience,
        Boolean acceptingNewPatients,
        Boolean telehealth,
        @Size(max = 80) String nextAvailable,
        @Size(max = 80) String externalProvider,
        @Size(max = 180) String externalId,
        @Size(max = 1000) String externalUri,
        Set<String> languages,
        Set<String> acceptedInsurances,
        Set<String> profileTags
) {
}
