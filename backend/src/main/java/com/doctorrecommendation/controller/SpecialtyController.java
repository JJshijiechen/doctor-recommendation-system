package com.doctorrecommendation.controller;

import com.doctorrecommendation.dto.SpecialtyResponse;
import com.doctorrecommendation.repository.SpecialtyRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/specialties")
public class SpecialtyController {

    private final SpecialtyRepository specialtyRepository;

    public SpecialtyController(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    @GetMapping
    public List<SpecialtyResponse> list() {
        return specialtyRepository.findAll().stream()
                .map(SpecialtyResponse::from)
                .toList();
    }
}
