package com.doctorrecommendation.repository;

import com.doctorrecommendation.domain.Doctor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @Override
    @EntityGraph(attributePaths = {"specialty", "languages", "acceptedInsurances", "profileTags"})
    List<Doctor> findAll();

    @EntityGraph(attributePaths = {"specialty", "languages", "acceptedInsurances", "profileTags"})
    List<Doctor> findAllByAcceptingNewPatientsTrue();

    Optional<Doctor> findByExternalProviderAndExternalId(String externalProvider, String externalId);
}
