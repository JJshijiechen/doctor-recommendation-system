package com.doctorrecommendation.service;

import com.doctorrecommendation.dto.TermExtractionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MedicalTermExtractionService {

    private final String apiKey;
    private final String model;
    private final LocalMedicalTermExtractor localExtractor;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MedicalTermExtractionService(
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.model:gpt-4.1-mini}") String model,
            LocalMedicalTermExtractor localExtractor,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.localExtractor = localExtractor;
        this.restClient = restClientBuilder.baseUrl("https://api.openai.com").build();
        this.objectMapper = objectMapper;
    }

    public TermExtractionResult extract(String symptoms) {
        if (apiKey == null || apiKey.isBlank()) {
            return localExtractor.extract(symptoms);
        }

        try {
            return extractWithOpenAi(symptoms);
        } catch (RuntimeException exception) {
            return localExtractor.extract(symptoms);
        }
    }

    private TermExtractionResult extractWithOpenAi(String symptoms) {
        String prompt = """
                Extract patient-facing medical terms and likely doctor specialties from the symptom text.
                Return only compact JSON with two arrays: terms and specialties.
                Use standard US specialty names. Do not diagnose.

                Symptom text:
                %s
                """.formatted(symptoms);

        Map<String, Object> body = Map.of(
                "model", model,
                "input", prompt
        );

        String response = restClient.post()
                .uri("/v1/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            String outputText = extractOutputText(response);
            JsonNode payload = objectMapper.readTree(extractJsonObject(outputText));
            List<String> terms = readArray(payload, "terms");
            List<String> specialties = readArray(payload, "specialties");
            if (terms.isEmpty() || specialties.isEmpty()) {
                throw new RestClientException("OpenAI extraction response did not include terms and specialties");
            }
            return new TermExtractionResult(terms, specialties, "openai");
        } catch (Exception exception) {
            throw new RestClientException("Unable to parse OpenAI extraction response", exception);
        }
    }

    private String extractOutputText(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        String outputText = root.path("output_text").asText(null);
        if (outputText != null && !outputText.isBlank()) {
            return outputText;
        }

        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode outputItem : output) {
                JsonNode content = outputItem.path("content");
                if (content.isArray()) {
                    for (JsonNode contentItem : content) {
                        String text = contentItem.path("text").asText(null);
                        if (text != null && !text.isBlank()) {
                            return text;
                        }
                    }
                }
            }
        }
        throw new RestClientException("OpenAI response did not contain output text");
    }

    private String extractJsonObject(String value) {
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new RestClientException("OpenAI response did not contain a JSON object");
        }
        return value.substring(start, end + 1);
    }

    private List<String> readArray(JsonNode payload, String field) {
        Set<String> values = new LinkedHashSet<>();
        JsonNode node = payload.path(field);
        if (!node.isArray()) {
            return List.of();
        }
        for (JsonNode item : node) {
            String value = item.asText("").trim();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return new ArrayList<>(values);
    }
}
