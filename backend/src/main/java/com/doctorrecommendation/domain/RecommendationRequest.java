package com.doctorrecommendation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "recommendation_requests")
public class RecommendationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_user_id")
    private PatientUser patientUser;

    @Lob
    @Column(nullable = false)
    private String symptomText;

    private Double latitude;

    private Double longitude;

    private Double radiusMiles;

    private String insurance;

    private String language;

    private Boolean telehealthPreferred;

    @Column(length = 1200)
    private String extractedTerms;

    @Column(nullable = false)
    private Instant requestedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public PatientUser getPatientUser() {
        return patientUser;
    }

    public void setPatientUser(PatientUser patientUser) {
        this.patientUser = patientUser;
    }

    public String getSymptomText() {
        return symptomText;
    }

    public void setSymptomText(String symptomText) {
        this.symptomText = symptomText;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getRadiusMiles() {
        return radiusMiles;
    }

    public void setRadiusMiles(Double radiusMiles) {
        this.radiusMiles = radiusMiles;
    }

    public String getInsurance() {
        return insurance;
    }

    public void setInsurance(String insurance) {
        this.insurance = insurance;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Boolean getTelehealthPreferred() {
        return telehealthPreferred;
    }

    public void setTelehealthPreferred(Boolean telehealthPreferred) {
        this.telehealthPreferred = telehealthPreferred;
    }

    public String getExtractedTerms() {
        return extractedTerms;
    }

    public void setExtractedTerms(String extractedTerms) {
        this.extractedTerms = extractedTerms;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }
}
