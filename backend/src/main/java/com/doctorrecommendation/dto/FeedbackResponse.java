package com.doctorrecommendation.dto;

import com.doctorrecommendation.domain.UserFeedback;

import java.time.Instant;

public record FeedbackResponse(
        Long id,
        Long recommendationRequestId,
        Long doctorId,
        int helpfulRating,
        boolean wouldContactProvider,
        String comment,
        Instant createdAt
) {
    public static FeedbackResponse from(UserFeedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getRecommendationRequestId(),
                feedback.getDoctorId(),
                feedback.getHelpfulRating(),
                feedback.isWouldContactProvider(),
                feedback.getComment(),
                feedback.getCreatedAt()
        );
    }
}
