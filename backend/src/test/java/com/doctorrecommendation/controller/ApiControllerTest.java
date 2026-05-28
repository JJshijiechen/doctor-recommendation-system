package com.doctorrecommendation.controller;

import com.doctorrecommendation.dto.DoctorResponse;
import com.doctorrecommendation.dto.FeedbackRequest;
import com.doctorrecommendation.dto.FeedbackResponse;
import com.doctorrecommendation.dto.LocationSearchResponse;
import com.doctorrecommendation.dto.RecommendationRequestDto;
import com.doctorrecommendation.dto.RecommendationResponse;
import com.doctorrecommendation.service.DoctorService;
import com.doctorrecommendation.service.FeedbackService;
import com.doctorrecommendation.service.LocationSearchService;
import com.doctorrecommendation.service.RecommendationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {DoctorController.class, RecommendationController.class, FeedbackController.class, LocationController.class})
@AutoConfigureMockMvc(addFilters = false)
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DoctorService doctorService;

    @MockBean
    private RecommendationService recommendationService;

    @MockBean
    private FeedbackService feedbackService;

    @MockBean
    private LocationSearchService locationSearchService;

    @Test
    void listsDoctors() throws Exception {
        when(doctorService.findAll()).thenReturn(List.of(doctor()));

        mockMvc.perform(get("/api/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Northwestern Memorial Hospital Cardiology"))
                .andExpect(jsonPath("$[0].specialty").value("Cardiology"));
    }

    @Test
    void postsRecommendationRequest() throws Exception {
        RecommendationResponse response = new RecommendationResponse(
                10L,
                Instant.now(),
                false,
                "local-dictionary",
                List.of("chest pain"),
                List.of("Cardiology"),
                List.of(),
                "Not a diagnosis"
        );
        when(recommendationService.recommend(any())).thenReturn(response);

        RecommendationRequestDto request = new RecommendationRequestDto("chest pain", 41.0, -87.0, 20.0, "Aetna", "English", true);

        mockMvc.perform(post("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(10))
                .andExpect(jsonPath("$.predictedSpecialties[0]").value("Cardiology"));
    }

    @Test
    void postsRecommendationFeedback() throws Exception {
        FeedbackResponse response = new FeedbackResponse(
                99L,
                10L,
                1L,
                5,
                true,
                "Good match",
                Instant.now()
        );
        when(feedbackService.submit(any())).thenReturn(response);

        FeedbackRequest request = new FeedbackRequest(10L, 1L, 5, true, "Good match");

        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.helpfulRating").value(5));
    }

    @Test
    void searchesLocations() throws Exception {
        when(locationSearchService.search("Boston Children's Hospital")).thenReturn(List.of(
                new LocationSearchResponse("Boston Children's Hospital", "300 Longwood Ave, Boston, MA", 42.3371, -71.1056, "serpapi-google-maps")
        ));

        mockMvc.perform(get("/api/locations/search")
                        .queryParam("query", "Boston Children's Hospital"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("Boston Children's Hospital"))
                .andExpect(jsonPath("$[0].latitude").value(42.3371))
                .andExpect(jsonPath("$[0].source").value("serpapi-google-maps"));
    }

    private DoctorResponse doctor() {
        return new DoctorResponse(
                1L,
                "Northwestern Memorial Hospital Cardiology",
                "Cardiology",
                "Northwestern Memorial Hospital Cardiology",
                "Live provider listing imported from SerpAPI Google Maps for cardiology care.",
                "675 N St Clair St",
                "Chicago",
                "IL",
                "60611",
                41.8949,
                -87.6220,
                null,
                4.3,
                null,
                true,
                true,
                "Call to confirm",
                "serpapi-google-maps",
                "test-serpapi-place",
                "https://www.google.com/maps/search/?api=1&query=Northwestern%20Memorial%20Hospital%20Cardiology",
                new LinkedHashSet<>(List.of("English")),
                new LinkedHashSet<>(List.of("Aetna")),
                new LinkedHashSet<>(List.of("chest pain"))
        );
    }
}
