package com.doctorrecommendation.service;

import com.doctorrecommendation.dto.TermExtractionResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class LocalMedicalTermExtractor {

    private static final List<TermRule> RULES = List.of(
            new TermRule("chest pain", "Cardiology", List.of("chest pain", "tight chest", "heart pain", "pressure in chest")),
            new TermRule("shortness of breath", "Pulmonology", List.of("shortness of breath", "difficulty breathing", "wheezing", "breathless")),
            new TermRule("shortness of breath", "Cardiology", List.of("shortness of breath", "difficulty breathing", "breathless")),
            new TermRule("skin rash", "Dermatology", List.of("rash", "itchy skin", "eczema", "hives", "acne")),
            new TermRule("headache", "Neurology", List.of("headache", "migraine", "dizziness", "numbness", "tingling")),
            new TermRule("joint pain", "Orthopedics", List.of("joint pain", "back pain", "knee pain", "shoulder pain", "sprain")),
            new TermRule("stomach pain", "Gastroenterology", List.of("stomach pain", "abdominal pain", "nausea", "vomiting", "acid reflux")),
            new TermRule("anxiety", "Psychiatry", List.of("anxiety", "panic", "depression", "insomnia", "stress")),
            new TermRule("pregnancy", "Obstetrics and Gynecology", List.of("pregnancy", "prenatal", "pelvic pain", "period pain", "menstrual")),
            new TermRule("child fever", "Pediatrics", List.of("child fever", "pediatric", "baby fever", "infant", "toddler")),
            new TermRule("diabetes", "Endocrinology", List.of("diabetes", "blood sugar", "thyroid", "hormone")),
            new TermRule("allergy", "Allergy and Immunology", List.of("allergy", "allergic", "sinus", "sneezing", "asthma")),
            new TermRule("fever", "Family Medicine", List.of("fever", "cold", "flu", "cough", "sore throat")),
            new TermRule("urgent symptoms", "Urgent Care", List.of("severe", "urgent", "sudden", "cannot breathe", "fainting"))
    );

    public TermExtractionResult extract(String symptoms) {
        String normalized = symptoms == null ? "" : symptoms.toLowerCase(Locale.ROOT);
        Set<String> terms = new LinkedHashSet<>();
        Set<String> specialties = new LinkedHashSet<>();

        for (TermRule rule : RULES) {
            if (rule.matches(normalized)) {
                terms.add(rule.term());
                specialties.add(rule.specialty());
            }
        }

        if (terms.isEmpty()) {
            terms.add("general symptoms");
            specialties.add("Family Medicine");
            specialties.add("Internal Medicine");
        }

        return new TermExtractionResult(List.copyOf(terms), List.copyOf(specialties), "local-dictionary");
    }

    private record TermRule(String term, String specialty, List<String> synonyms) {
        private boolean matches(String value) {
            return synonyms.stream().anyMatch(value::contains);
        }
    }
}
