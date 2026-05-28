package com.doctorrecommendation.dto;

import java.util.List;

public record TermExtractionResult(
        List<String> terms,
        List<String> specialties,
        String source
) {
}
