package com.doctorrecommendation.dto;

import com.doctorrecommendation.domain.Doctor;

import java.util.Set;

public record DoctorResponse(
        Long id,
        String fullName,
        String specialty,
        String clinicName,
        String bio,
        String addressLine,
        String city,
        String state,
        String postalCode,
        Double latitude,
        Double longitude,
        String phone,
        Double rating,
        Integer yearsExperience,
        boolean acceptingNewPatients,
        boolean telehealth,
        String nextAvailable,
        String externalProvider,
        String externalId,
        String externalUri,
        Set<String> languages,
        Set<String> acceptedInsurances,
        Set<String> profileTags
) {
    public static DoctorResponse from(Doctor doctor) {
        return new DoctorResponse(
                doctor.getId(),
                doctor.getFullName(),
                doctor.getSpecialty().getName(),
                doctor.getClinicName(),
                doctor.getBio(),
                doctor.getAddressLine(),
                doctor.getCity(),
                doctor.getState(),
                doctor.getPostalCode(),
                doctor.getLatitude(),
                doctor.getLongitude(),
                doctor.getPhone(),
                doctor.getRating(),
                doctor.getYearsExperience(),
                doctor.isAcceptingNewPatients(),
                doctor.isTelehealth(),
                doctor.getNextAvailable(),
                doctor.getExternalProvider(),
                doctor.getExternalId(),
                doctor.getExternalUri(),
                doctor.getLanguages(),
                doctor.getAcceptedInsurances(),
                doctor.getProfileTags()
        );
    }
}
