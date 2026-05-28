package com.doctorrecommendation.dto;

public record RecommendationResultResponse(
        int rank,
        double matchScore,
        String reason,
        Double distanceMiles,
        String placesSource,
        String placeName,
        String placeAddress,
        DoctorResponse doctor
) {
}
