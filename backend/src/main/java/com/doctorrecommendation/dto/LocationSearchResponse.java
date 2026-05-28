package com.doctorrecommendation.dto;

public record LocationSearchResponse(
        String label,
        String address,
        Double latitude,
        Double longitude,
        String source
) {
}
