package com.doctorrecommendation.dto;

import java.time.Instant;
import java.util.List;

public record RecommendationResponse(
        Long requestId,
        Instant requestedAt,
        boolean cacheHit,
        String extractionSource,
        List<String> extractedTerms,
        List<String> predictedSpecialties,
        List<RecommendationResultResponse> results,
        String disclaimer
) {
}
