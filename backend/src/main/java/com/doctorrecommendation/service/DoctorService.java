package com.doctorrecommendation.service;

import com.doctorrecommendation.domain.Doctor;
import com.doctorrecommendation.domain.Specialty;
import com.doctorrecommendation.dto.CreateDoctorRequest;
import com.doctorrecommendation.dto.DoctorResponse;
import com.doctorrecommendation.repository.DoctorRepository;
import com.doctorrecommendation.repository.SpecialtyRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;

    public DoctorService(DoctorRepository doctorRepository, SpecialtyRepository specialtyRepository) {
        this.doctorRepository = doctorRepository;
        this.specialtyRepository = specialtyRepository;
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> findAll() {
        return doctorRepository.findAll().stream().map(DoctorResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DoctorResponse findById(Long id) {
        return doctorRepository.findById(id)
                .map(DoctorResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found: " + id));
    }

    @Transactional
    public DoctorResponse create(CreateDoctorRequest request) {
        Specialty specialty = specialtyRepository.findByNameIgnoreCase(request.specialty())
                .orElseGet(() -> specialtyRepository.save(new Specialty(request.specialty(), "Custom specialty")));

        Doctor doctor = new Doctor();
        doctor.setFullName(request.fullName());
        doctor.setSpecialty(specialty);
        doctor.setClinicName(request.clinicName());
        doctor.setBio(request.bio());
        doctor.setAddressLine(request.addressLine());
        doctor.setCity(request.city());
        doctor.setState(request.state());
        doctor.setPostalCode(request.postalCode());
        doctor.setLatitude(request.latitude());
        doctor.setLongitude(request.longitude());
        doctor.setPhone(request.phone());
        doctor.setRating(request.rating());
        doctor.setYearsExperience(request.yearsExperience());
        doctor.setAcceptingNewPatients(request.acceptingNewPatients() == null || request.acceptingNewPatients());
        doctor.setTelehealth(Boolean.TRUE.equals(request.telehealth()));
        doctor.setNextAvailable(request.nextAvailable());
        doctor.setExternalProvider(request.externalProvider());
        doctor.setExternalId(request.externalId());
        doctor.setExternalUri(request.externalUri());
        doctor.setLanguages(copyOrEmpty(request.languages()));
        doctor.setAcceptedInsurances(copyOrEmpty(request.acceptedInsurances()));
        doctor.setProfileTags(copyOrEmpty(request.profileTags()));
        return DoctorResponse.from(doctorRepository.save(doctor));
    }

    private Set<String> copyOrEmpty(Set<String> values) {
        return values == null ? new LinkedHashSet<>() : new LinkedHashSet<>(values);
    }
}
