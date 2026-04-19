package com.example.VehicleServiceManagementService.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleCleanupService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Xóa các bản ghi checkinoutlog liên quan đến xe.
     * Sử dụng REQUIRES_NEW để chạy trong một transaction độc lập.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteCheckInOutLogs(Long vehicleId) {
        int deletedCount = entityManager.createNativeQuery(
                "DELETE FROM legal_contract.checkinoutlog WHERE vehicle_id = :vehicleId")
                .setParameter("vehicleId", vehicleId)
                .executeUpdate();
        System.out.println("DEBUG [REQUIRES_NEW]: Đã xóa " + deletedCount + " checkinoutlog cho xe " + vehicleId);
    }

    /**
     * Xóa các bản ghi vehicleservice liên quan đến xe.
     * Sử dụng REQUIRES_NEW để chạy trong một transaction độc lập.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteVehicleServices(Long vehicleId) {
        int deletedCount = entityManager.createNativeQuery(
                "DELETE FROM vehicle_management.vehicleservice WHERE vehicle_id = :vehicleId")
                .setParameter("vehicleId", vehicleId)
                .executeUpdate();
        System.out.println("DEBUG [REQUIRES_NEW]: Đã xóa " + deletedCount + " vehicleservice cho xe " + vehicleId);
    }
}
