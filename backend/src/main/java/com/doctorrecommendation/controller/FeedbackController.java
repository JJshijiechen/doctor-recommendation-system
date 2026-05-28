package com.doctorrecommendation.controller;

import com.doctorrecommendation.dto.FeedbackRequest;
import com.doctorrecommendation.dto.FeedbackResponse;
import com.doctorrecommendation.dto.FeedbackSummaryResponse;
import com.doctorrecommendation.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse submit(@Valid @RequestBody FeedbackRequest request) {
        return feedbackService.submit(request);
    }

    @GetMapping("/summary")
    public FeedbackSummaryResponse summary() {
        return feedbackService.summary();
    }
}
