package com.example.reservationadminservice.controller;

import com.example.reservationadminservice.model.VehicleAdmin;
import com.example.reservationadminservice.service.AdminVehicleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vehicles")
@CrossOrigin(origins = "http://localhost:8080")
public class AdminVehicleController {

    private final AdminVehicleService service;

    public AdminVehicleController(AdminVehicleService service) {
        this.service = service;
    }

    @GetMapping
    public List<VehicleAdmin> getAll() {
        return service.getAllVehicles();
    }
}
