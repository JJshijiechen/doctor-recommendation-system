package com.doctorrecommendation.repository;

import com.doctorrecommendation.domain.SymptomTerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SymptomTermRepository extends JpaRepository<SymptomTerm, Long> {
    Optional<SymptomTerm> findByTermIgnoreCase(String term);
}
