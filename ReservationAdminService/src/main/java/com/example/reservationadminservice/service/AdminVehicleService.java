package com.example.reservationadminservice.service;

import com.example.reservationadminservice.model.VehicleAdmin;
import com.example.reservationadminservice.repository.admin.AdminVehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminVehicleService {

    private final AdminVehicleRepository repository;

    public AdminVehicleService(AdminVehicleRepository repository) {
        this.repository = repository;
    }

    public List<VehicleAdmin> getAllVehicles() {
        return repository.findAll();
    }
    
    public VehicleAdmin getVehicleById(Long id) {
        return repository.findById(id).orElse(null);
    }
    
    public List<VehicleAdmin> getVehiclesByGroupId(Long groupId) {
        return repository.findByGroupId(groupId);
    }
}
