package com.example.reservationservice.controller;

import com.example.reservationservice.dto.FairnessSuggestionRequest;
import com.example.reservationservice.dto.FairnessSuggestionResponse;
import com.example.reservationservice.dto.FairnessSummaryDTO;
import com.example.reservationservice.service.FairnessEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fairness")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8080"}, allowCredentials = "true")
public class FairnessController {

    private final FairnessEngineService fairnessEngineService;

    @GetMapping("/vehicles/{vehicleId}")
    public FairnessSummaryDTO getFairnessSummary(
            @PathVariable Integer vehicleId,
            @RequestParam(required = false, defaultValue = "30") Integer rangeDays,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = authHeader != null ? authHeader : null;
        return fairnessEngineService.buildSummary(vehicleId, rangeDays, token);
    }

    @PostMapping("/vehicles/{vehicleId}/suggest")
    public FairnessSuggestionResponse suggest(
            @PathVariable Integer vehicleId,
            @RequestBody FairnessSuggestionRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = authHeader != null ? authHeader : null;
        return fairnessEngineService.suggest(vehicleId, request, token);
    }
}

