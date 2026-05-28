package com.doctorrecommendation.repository;

import com.doctorrecommendation.domain.UserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {

    long countByWouldContactProviderTrue();
}
