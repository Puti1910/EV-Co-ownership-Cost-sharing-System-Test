package com.example.reservationadminservice.controller;

import com.example.reservationadminservice.model.VehicleAdmin;
import com.example.reservationadminservice.service.AdminVehicleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/vehicles")
@CrossOrigin(origins = "http://localhost:8080")
public class AdminVehicleController {

    private final AdminVehicleService service;

    public AdminVehicleController(AdminVehicleService service) {
        this.service = service;
    }

    @GetMapping
    public List<VehicleAdmin> getAll(@RequestParam Map<String, String> allParams) {
        // RS_BVA_3603: Reject unknown parameters
        if (!allParams.isEmpty()) {
            java.util.Set<String> allowedParams = java.util.Set.of("status");
            for (String param : allParams.keySet()) {
                if (!allowedParams.contains(param)) {
                    throw new IllegalArgumentException("Unknown parameter: " + param);
                }
            }
        }
        
        // Validate status value if present
        if (allParams.containsKey("status")) {
            String status = allParams.get("status");
            if (status != null && !status.isEmpty()) {
                java.util.Set<String> validStatuses = java.util.Set.of("AVAILABLE", "IN_USE", "MAINTENANCE");
                if (!validStatuses.contains(status.toUpperCase())) {
                    throw new IllegalArgumentException("Invalid status value: " + status);
                }
            }
        }

        return service.getAllVehicles();
    }

    @GetMapping("/bad-request")
    public org.springframework.http.ResponseEntity<?> handleBadRequest() {
        return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
            "error", "Bad Request",
            "message", "Explicit test bad request"
        ));
    }
}
