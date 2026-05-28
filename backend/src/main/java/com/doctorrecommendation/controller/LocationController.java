package com.doctorrecommendation.controller;

import com.doctorrecommendation.dto.LocationSearchResponse;
import com.doctorrecommendation.service.LocationSearchService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationSearchService locationSearchService;

    public LocationController(LocationSearchService locationSearchService) {
        this.locationSearchService = locationSearchService;
    }

    @GetMapping("/search")
    public List<LocationSearchResponse> search(
            @RequestParam @NotBlank @Size(max = 160) String query
    ) {
        return locationSearchService.search(query);
    }
}
