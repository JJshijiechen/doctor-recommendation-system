package com.doctorrecommendation.repository;

import com.doctorrecommendation.domain.PatientUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientUserRepository extends JpaRepository<PatientUser, Long> {
    Optional<PatientUser> findByOauthSubject(String oauthSubject);
}
