package com.doctorrecommendation.service;

import com.doctorrecommendation.domain.UserFeedback;
import com.doctorrecommendation.dto.FeedbackRequest;
import com.doctorrecommendation.dto.FeedbackResponse;
import com.doctorrecommendation.dto.FeedbackSummaryResponse;
import com.doctorrecommendation.repository.UserFeedbackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {

    private final UserFeedbackRepository feedbackRepository;

    public FeedbackService(UserFeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional
    public FeedbackResponse submit(FeedbackRequest request) {
        UserFeedback feedback = new UserFeedback();
        feedback.setRecommendationRequestId(request.recommendationRequestId());
        feedback.setDoctorId(request.doctorId());
        feedback.setHelpfulRating(request.helpfulRating());
        feedback.setWouldContactProvider(request.wouldContactProvider());
        feedback.setComment(request.comment());
        return FeedbackResponse.from(feedbackRepository.save(feedback));
    }

    @Transactional(readOnly = true)
    public FeedbackSummaryResponse summary() {
        long total = feedbackRepository.count();
        double average = feedbackRepository.findAll().stream()
                .mapToInt(UserFeedback::getHelpfulRating)
                .average()
                .orElse(0.0);
        return new FeedbackSummaryResponse(total, average, feedbackRepository.countByWouldContactProviderTrue());
    }
}
