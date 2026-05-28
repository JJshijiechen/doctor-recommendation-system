package com.doctorrecommendation.controller;

import com.doctorrecommendation.dto.RecommendationHistoryResponse;
import com.doctorrecommendation.dto.RecommendationRequestDto;
import com.doctorrecommendation.dto.RecommendationResponse;
import com.doctorrecommendation.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public RecommendationResponse recommend(@Valid @RequestBody RecommendationRequestDto request) {
        return recommendationService.recommend(request);
    }

    @GetMapping("/history")
    public List<RecommendationHistoryResponse> history() {
        return recommendationService.history();
    }
}
