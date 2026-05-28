package com.doctorrecommendation.dto;

import java.time.Instant;

public record RecommendationHistoryResponse(
        Long requestId,
        String symptoms,
        String extractedTerms,
        String insurance,
        String language,
        Boolean telehealthPreferred,
        Instant requestedAt,
        int resultCount
) {
}
