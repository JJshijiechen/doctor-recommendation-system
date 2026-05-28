package com.doctorrecommendation.service;

import java.util.Set;

public record DiscoveredDoctorProfile(
        String externalProvider,
        String externalId,
        String externalUri,
        String displayName,
        String specialtyName,
        String formattedAddress,
        Double latitude,
        Double longitude,
        String phone,
        Double rating,
        Set<String> tags
) {
}
