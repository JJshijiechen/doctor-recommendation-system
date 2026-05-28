package com.doctorrecommendation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecommendationRequestDto(
        @NotBlank @Size(max = 2000) String symptoms,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @DecimalMin("1.0") @DecimalMax("100.0") Double radiusMiles,
        @Size(max = 80) String insurance,
        @Size(max = 80) String language,
        Boolean telehealthPreferred
) {
    public double effectiveRadiusMiles() {
        return radiusMiles == null ? 25.0 : radiusMiles;
    }
}
