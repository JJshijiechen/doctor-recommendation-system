package com.doctorrecommendation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(
        @NotNull Long recommendationRequestId,
        @NotNull Long doctorId,
        @Min(1) @Max(5) int helpfulRating,
        boolean wouldContactProvider,
        @Size(max = 1200) String comment
) {
}
