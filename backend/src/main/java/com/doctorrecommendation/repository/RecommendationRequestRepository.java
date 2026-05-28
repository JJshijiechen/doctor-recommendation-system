package com.doctorrecommendation.repository;

import com.doctorrecommendation.domain.RecommendationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationRequestRepository extends JpaRepository<RecommendationRequest, Long> {
    List<RecommendationRequest> findTop10ByOrderByRequestedAtDesc();
}
