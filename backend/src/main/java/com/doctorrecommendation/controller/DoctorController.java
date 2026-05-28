package com.doctorrecommendation.controller;

import com.doctorrecommendation.dto.CreateDoctorRequest;
import com.doctorrecommendation.dto.DoctorResponse;
import com.doctorrecommendation.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping("/doctors")
    public List<DoctorResponse> listDoctors() {
        return doctorService.findAll();
    }

    @GetMapping("/doctors/{id}")
    public DoctorResponse getDoctor(@PathVariable Long id) {
        return doctorService.findById(id);
    }

    @PostMapping("/admin/doctors")
    @ResponseStatus(HttpStatus.CREATED)
    public DoctorResponse createDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        return doctorService.create(request);
    }
}
