package com.doctorrecommendation.dto;

public record FeedbackSummaryResponse(
        long totalResponses,
        double averageHelpfulRating,
        long wouldContactProviderCount
) {
}
