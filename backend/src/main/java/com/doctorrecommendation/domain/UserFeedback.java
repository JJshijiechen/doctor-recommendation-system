package com.doctorrecommendation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_feedback")
public class UserFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long recommendationRequestId;

    private Long doctorId;

    @Column(nullable = false)
    private int helpfulRating;

    @Column(nullable = false)
    private boolean wouldContactProvider;

    @Column(length = 1200)
    private String comment;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Long getRecommendationRequestId() {
        return recommendationRequestId;
    }

    public void setRecommendationRequestId(Long recommendationRequestId) {
        this.recommendationRequestId = recommendationRequestId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public int getHelpfulRating() {
        return helpfulRating;
    }

    public void setHelpfulRating(int helpfulRating) {
        this.helpfulRating = helpfulRating;
    }

    public boolean isWouldContactProvider() {
        return wouldContactProvider;
    }

    public void setWouldContactProvider(boolean wouldContactProvider) {
        this.wouldContactProvider = wouldContactProvider;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
