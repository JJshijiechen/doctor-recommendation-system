package com.doctorrecommendation.repository;

import com.doctorrecommendation.domain.RecommendationResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationResultRepository extends JpaRepository<RecommendationResult, Long> {

    @EntityGraph(attributePaths = {"doctor", "doctor.specialty", "doctor.languages", "doctor.acceptedInsurances", "doctor.profileTags"})
    List<RecommendationResult> findByRequestIdOrderByRankPositionAsc(Long requestId);
}
