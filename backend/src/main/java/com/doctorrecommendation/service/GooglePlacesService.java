package com.doctorrecommendation.service;

import com.doctorrecommendation.domain.Doctor;
import org.springframework.stereotype.Service;

@Service
public class GooglePlacesService {

    public PlaceEnrichment enrich(Doctor doctor) {
        if (hasText(doctor.getExternalProvider())) {
            return new PlaceEnrichment(doctor.getExternalProvider(), doctor.getClinicName(), fullAddress(doctor));
        }
        return fallback(doctor);
    }

    private PlaceEnrichment fallback(Doctor doctor) {
        return new PlaceEnrichment("stored-provider", doctor.getClinicName(), fullAddress(doctor));
    }

    private String fullAddress(Doctor doctor) {
        return doctor.getAddressLine() + ", " + doctor.getCity() + ", " + doctor.getState() + " " + doctor.getPostalCode();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
