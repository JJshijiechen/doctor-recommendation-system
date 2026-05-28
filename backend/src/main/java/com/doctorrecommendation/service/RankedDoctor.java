package com.doctorrecommendation.service;

import com.doctorrecommendation.domain.Doctor;

public record RankedDoctor(
        Doctor doctor,
        double score,
        Double distanceMiles,
        String reason
) {
}
