package com.doctorrecommendation.dto;

import com.doctorrecommendation.domain.Specialty;

public record SpecialtyResponse(
        Long id,
        String name,
        String description
) {
    public static SpecialtyResponse from(Specialty specialty) {
        return new SpecialtyResponse(specialty.getId(), specialty.getName(), specialty.getDescription());
    }
}
