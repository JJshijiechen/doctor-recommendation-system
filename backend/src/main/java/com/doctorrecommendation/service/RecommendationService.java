package com.doctorrecommendation.service;

import com.doctorrecommendation.domain.Doctor;
import com.doctorrecommendation.domain.RecommendationRequest;
import com.doctorrecommendation.domain.RecommendationResult;
import com.doctorrecommendation.dto.DoctorResponse;
import com.doctorrecommendation.dto.RecommendationHistoryResponse;
import com.doctorrecommendation.dto.RecommendationRequestDto;
import com.doctorrecommendation.dto.RecommendationResponse;
import com.doctorrecommendation.dto.RecommendationResultResponse;
import com.doctorrecommendation.dto.TermExtractionResult;
import com.doctorrecommendation.repository.DoctorRepository;
import com.doctorrecommendation.repository.RecommendationRequestRepository;
import com.doctorrecommendation.repository.RecommendationResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class RecommendationService {

    private static final String DISCLAIMER = "This is not a diagnosis. Use these recommendations to choose care and contact emergency services for urgent symptoms.";

    private final DoctorRepository doctorRepository;
    private final RecommendationRequestRepository requestRepository;
    private final RecommendationResultRepository resultRepository;
    private final MedicalTermExtractionService extractionService;
    private final TfIdfRankingService rankingService;
    private final GooglePlacesService placesService;
    private final RecommendationCacheService cacheService;
    private final DoctorDiscoveryService doctorDiscoveryService;

    public RecommendationService(
            DoctorRepository doctorRepository,
            RecommendationRequestRepository requestRepository,
            RecommendationResultRepository resultRepository,
            MedicalTermExtractionService extractionService,
            TfIdfRankingService rankingService,
            GooglePlacesService placesService,
            RecommendationCacheService cacheService,
            DoctorDiscoveryService doctorDiscoveryService
    ) {
        this.doctorRepository = doctorRepository;
        this.requestRepository = requestRepository;
        this.resultRepository = resultRepository;
        this.extractionService = extractionService;
        this.rankingService = rankingService;
        this.placesService = placesService;
        this.cacheService = cacheService;
        this.doctorDiscoveryService = doctorDiscoveryService;
    }

    @Transactional
    public RecommendationResponse recommend(RecommendationRequestDto requestDto) {
        String cacheKey = cacheService.keyFor(requestDto);
        return cacheService.get(cacheKey)
                .map(this::markCacheHit)
                .orElseGet(() -> generateAndCache(requestDto, cacheKey));
    }

    @Transactional(readOnly = true)
    public List<RecommendationHistoryResponse> history() {
        return requestRepository.findTop10ByOrderByRequestedAtDesc().stream()
                .map(request -> new RecommendationHistoryResponse(
                        request.getId(),
                        request.getSymptomText(),
                        request.getExtractedTerms(),
                        request.getInsurance(),
                        request.getLanguage(),
                        request.getTelehealthPreferred(),
                        request.getRequestedAt(),
                        resultRepository.findByRequestIdOrderByRankPositionAsc(request.getId()).size()
                ))
                .toList();
    }

    private RecommendationResponse generateAndCache(RecommendationRequestDto requestDto, String cacheKey) {
        TermExtractionResult extraction = extractionService.extract(requestDto.symptoms());
        RecommendationRequest request = persistRequest(requestDto, extraction);
        doctorDiscoveryService.discoverAndSave(requestDto, extraction);
        List<Doctor> doctors = doctorRepository.findAllByAcceptingNewPatientsTrue();
        List<RankedDoctor> rankedDoctors = rankingService.rank(doctors, extraction, requestDto).stream()
                .limit(8)
                .toList();

        List<RecommendationResultResponse> responses = persistResults(request, rankedDoctors);
        RecommendationResponse response = new RecommendationResponse(
                request.getId(),
                request.getRequestedAt(),
                false,
                extraction.source(),
                extraction.terms(),
                extraction.specialties(),
                responses,
                DISCLAIMER
        );
        cacheService.put(cacheKey, response);
        return response;
    }

    private RecommendationRequest persistRequest(RecommendationRequestDto requestDto, TermExtractionResult extraction) {
        RecommendationRequest request = new RecommendationRequest();
        request.setSymptomText(requestDto.symptoms());
        request.setLatitude(requestDto.latitude());
        request.setLongitude(requestDto.longitude());
        request.setRadiusMiles(requestDto.effectiveRadiusMiles());
        request.setInsurance(requestDto.insurance());
        request.setLanguage(requestDto.language());
        request.setTelehealthPreferred(requestDto.telehealthPreferred());
        request.setExtractedTerms(String.join(", ", extraction.terms()));
        request.setRequestedAt(Instant.now());
        return requestRepository.save(request);
    }

    private List<RecommendationResultResponse> persistResults(RecommendationRequest request, List<RankedDoctor> rankedDoctors) {
        List<RecommendationResultResponse> responses = new java.util.ArrayList<>();
        int rank = 1;
        for (RankedDoctor rankedDoctor : rankedDoctors) {
            PlaceEnrichment place = placesService.enrich(rankedDoctor.doctor());
            RecommendationResult result = new RecommendationResult();
            result.setRequest(request);
            result.setDoctor(rankedDoctor.doctor());
            result.setMatchScore(rankedDoctor.score());
            result.setRankPosition(rank);
            result.setReason(rankedDoctor.reason());
            result.setDistanceMiles(rankedDoctor.distanceMiles());
            result.setPlacesSource(place.source());
            result.setPlaceName(place.name());
            result.setPlaceAddress(place.address());
            resultRepository.save(result);

            responses.add(new RecommendationResultResponse(
                    rank,
                    rankedDoctor.score(),
                    rankedDoctor.reason(),
                    rankedDoctor.distanceMiles(),
                    place.source(),
                    place.name(),
                    place.address(),
                    DoctorResponse.from(rankedDoctor.doctor())
            ));
            rank++;
        }
        return responses;
    }

    private RecommendationResponse markCacheHit(RecommendationResponse response) {
        return new RecommendationResponse(
                response.requestId(),
                response.requestedAt(),
                true,
                response.extractionSource(),
                response.extractedTerms(),
                response.predictedSpecialties(),
                response.results(),
                response.disclaimer()
        );
    }
}
