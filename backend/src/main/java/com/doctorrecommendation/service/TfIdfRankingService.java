package com.doctorrecommendation.service;

import com.doctorrecommendation.domain.Doctor;
import com.doctorrecommendation.dto.RecommendationRequestDto;
import com.doctorrecommendation.dto.TermExtractionResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TfIdfRankingService {

    private static final Pattern TOKEN_SPLITTER = Pattern.compile("[^a-zA-Z0-9]+");

    public List<RankedDoctor> rank(List<Doctor> doctors, TermExtractionResult extraction, RecommendationRequestDto request) {
        if (doctors.isEmpty()) {
            return List.of();
        }

        Map<Doctor, List<String>> doctorTokens = doctors.stream()
                .collect(Collectors.toMap(doctor -> doctor, doctor -> tokenize(buildDoctorText(doctor))));
        List<String> queryTokens = tokenize(String.join(" ",
                request.symptoms(),
                String.join(" ", extraction.terms()),
                String.join(" ", extraction.specialties())
        ));
        Map<String, Double> idf = calculateIdf(doctorTokens.values());

        List<RankedDoctor> ranked = new ArrayList<>();
        for (Doctor doctor : doctors) {
            List<String> tokens = doctorTokens.get(doctor);
            double tfIdfScore = cosineLikeScore(queryTokens, tokens, idf);
            double score = tfIdfScore;

            if (specialtyMatches(doctor, extraction)) {
                score += 0.35;
            }
            if (doctor.isAcceptingNewPatients()) {
                score += 0.05;
            }
            if (Boolean.TRUE.equals(request.telehealthPreferred()) && doctor.isTelehealth()) {
                score += 0.08;
            }
            if (matchesCollection(doctor.getAcceptedInsurances(), request.insurance())) {
                score += 0.10;
            }
            if (matchesCollection(doctor.getLanguages(), request.language())) {
                score += 0.08;
            }
            if (doctor.getRating() != null) {
                score += Math.min(doctor.getRating() / 5.0, 1.0) * 0.08;
            }

            Double distance = distanceMiles(request.latitude(), request.longitude(), doctor.getLatitude(), doctor.getLongitude());
            if (distance != null) {
                double radius = request.effectiveRadiusMiles();
                if (distance <= radius) {
                    score += 0.15 * (1.0 - Math.min(distance / radius, 1.0));
                } else {
                    score -= 0.12;
                }
            }

            ranked.add(new RankedDoctor(doctor, round(score), distance == null ? null : round(distance), reasonFor(doctor, extraction, request, distance)));
        }

        return ranked.stream()
                .sorted(Comparator.comparingDouble(RankedDoctor::score).reversed())
                .toList();
    }

    private Map<String, Double> calculateIdf(Iterable<List<String>> documents) {
        List<Set<String>> uniqueDocs = new ArrayList<>();
        for (List<String> tokens : documents) {
            uniqueDocs.add(new HashSet<>(tokens));
        }

        Map<String, Double> idf = new HashMap<>();
        int totalDocs = uniqueDocs.size();
        Set<String> vocabulary = uniqueDocs.stream().flatMap(Set::stream).collect(Collectors.toSet());
        for (String token : vocabulary) {
            long docsWithTerm = uniqueDocs.stream().filter(doc -> doc.contains(token)).count();
            idf.put(token, Math.log((totalDocs + 1.0) / (docsWithTerm + 1.0)) + 1.0);
        }
        return idf;
    }

    private double cosineLikeScore(List<String> queryTokens, List<String> doctorTokens, Map<String, Double> idf) {
        if (queryTokens.isEmpty() || doctorTokens.isEmpty()) {
            return 0.0;
        }

        Map<String, Long> doctorTf = doctorTokens.stream().collect(Collectors.groupingBy(token -> token, Collectors.counting()));
        Set<String> querySet = new LinkedHashSet<>(queryTokens);

        double score = 0.0;
        double max = 0.0;
        for (String token : querySet) {
            double weight = idf.getOrDefault(token, 1.0);
            max += weight;
            score += Math.min(doctorTf.getOrDefault(token, 0L), 3L) / 3.0 * weight;
        }
        return max == 0.0 ? 0.0 : score / max;
    }

    private boolean specialtyMatches(Doctor doctor, TermExtractionResult extraction) {
        String specialty = normalize(doctor.getSpecialty().getName());
        return extraction.specialties().stream().map(this::normalize).anyMatch(specialty::contains)
                || extraction.specialties().stream().map(this::normalize).anyMatch(value -> value.contains(specialty));
    }

    private boolean matchesCollection(Set<String> values, String requested) {
        if (requested == null || requested.isBlank()) {
            return false;
        }
        String normalized = normalize(requested);
        return values.stream().map(this::normalize).anyMatch(value -> value.contains(normalized) || normalized.contains(value));
    }

    private String reasonFor(Doctor doctor, TermExtractionResult extraction, RecommendationRequestDto request, Double distance) {
        List<String> factors = new ArrayList<>();
        if (specialtyMatches(doctor, extraction)) {
            factors.add("specialty match: " + doctor.getSpecialty().getName());
        }
        List<String> matchedTerms = extraction.terms().stream()
                .filter(term -> buildDoctorText(doctor).toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT)))
                .toList();
        if (!matchedTerms.isEmpty()) {
            factors.add("symptom terms: " + String.join(", ", matchedTerms));
        }
        if (matchesCollection(doctor.getAcceptedInsurances(), request.insurance())) {
            factors.add("accepts " + request.insurance());
        }
        if (matchesCollection(doctor.getLanguages(), request.language())) {
            factors.add("language: " + request.language());
        }
        if (Boolean.TRUE.equals(request.telehealthPreferred()) && doctor.isTelehealth()) {
            factors.add("telehealth available");
        }
        if (distance != null) {
            factors.add("%.1f miles away".formatted(distance));
        }
        if (factors.isEmpty()) {
            factors.add("closest general profile match");
        }
        return String.join("; ", factors);
    }

    private List<String> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Stream.of(TOKEN_SPLITTER.split(value.toLowerCase(Locale.ROOT)))
                .filter(token -> token.length() > 2)
                .filter(token -> !Set.of("and", "the", "for", "with", "near", "from", "that").contains(token))
                .toList();
    }

    private String buildDoctorText(Doctor doctor) {
        return String.join(" ",
                nullToEmpty(doctor.getFullName()),
                nullToEmpty(doctor.getSpecialty().getName()),
                nullToEmpty(doctor.getClinicName()),
                nullToEmpty(doctor.getBio()),
                nullToEmpty(doctor.getCity()),
                String.join(" ", doctor.getProfileTags()),
                String.join(" ", doctor.getLanguages()),
                String.join(" ", doctor.getAcceptedInsurances())
        );
    }

    private Double distanceMiles(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return null;
        }
        double earthRadiusMiles = 3958.8;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusMiles * c;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
