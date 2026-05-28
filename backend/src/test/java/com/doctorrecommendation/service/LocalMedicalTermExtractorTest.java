package com.doctorrecommendation.service;

import com.doctorrecommendation.dto.TermExtractionResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalMedicalTermExtractorTest {

    private final LocalMedicalTermExtractor extractor = new LocalMedicalTermExtractor();

    @Test
    void extractsCardiologyAndPulmonologyTermsWithoutOpenAi() {
        TermExtractionResult result = extractor.extract("I have chest pain and shortness of breath near Chicago");

        assertThat(result.source()).isEqualTo("local-dictionary");
        assertThat(result.terms()).contains("chest pain", "shortness of breath");
        assertThat(result.specialties()).contains("Cardiology", "Pulmonology");
    }

    @Test
    void fallsBackToPrimaryCareForUnknownText() {
        TermExtractionResult result = extractor.extract("I am not sure what kind of visit I need");

        assertThat(result.terms()).contains("general symptoms");
        assertThat(result.specialties()).contains("Family Medicine", "Internal Medicine");
    }
}
