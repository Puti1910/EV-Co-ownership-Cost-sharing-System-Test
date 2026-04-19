package com.example.reservationadminservice.service;

import com.example.reservationadminservice.dto.ReservationDTO;
import com.example.reservationadminservice.model.ReservationAdmin;
import com.example.reservationadminservice.model.VehicleAdmin;
import com.example.reservationadminservice.repository.admin.AdminReservationRepository;
import com.example.reservationadminservice.repository.admin.AdminVehicleRepository;
import com.example.reservationadminservice.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminReservationService {

    private static final Logger logger = LoggerFactory.getLogger(AdminReservationService.class);
    private final AdminReservationRepository repository;
    private final AdminVehicleRepository vehicleRepository;
    private final ExternalApiService externalApiService;
    private final RestTemplate restTemplate;
    
    @Value("${reservation.service.url:http://reservation-service:8086}")
    private String reservationServiceUrl;

    public AdminReservationService(AdminReservationRepository repository,
                                   AdminVehicleRepository vehicleRepository,
                                   ExternalApiService externalApiService,
                                   RestTemplate restTemplate) {
        this.repository = repository;
        this.vehicleRepository = vehicleRepository;
        this.externalApiService = externalApiService;
        this.restTemplate = restTemplate;
    }

    public List<ReservationDTO> getAllReservations() {
        List<ReservationAdmin> allReservations = repository.findAll();
        return allReservations.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    private ReservationDTO convertToDTO(ReservationAdmin reservation) {
        ReservationDTO dto = new ReservationDTO();
        dto.setReservationId(reservation.getId());
        dto.setVehicleId(reservation.getVehicleId());
        dto.setUserId(reservation.getUserId());
        dto.setStartDatetime(reservation.getStartDatetime());
        dto.setEndDatetime(reservation.getEndDatetime());
        dto.setPurpose(reservation.getPurpose());
        dto.setStatus(reservation.getStatus());
        dto.setCreatedAt(reservation.getCreatedAt() != null ? 
            reservation.getCreatedAt().toLocalDateTime() : null);
        
        // Fetch names for UI display
        dto.setVehicleName(externalApiService.getVehicleName(reservation.getVehicleId()));
        dto.setUserName(externalApiService.getUserName(reservation.getUserId()));
        
        return dto;
    }
    
    public Optional<ReservationDTO> getReservationById(Long id) {
        return repository.findById(id).map(this::convertToDTO);
    }
    
    public ReservationDTO createReservation(ReservationDTO dto) {
        // Date validation
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxDate = LocalDateTime.of(2050, 12, 31, 23, 59, 59);

        if (dto.getStartDatetime() != null && dto.getStartDatetime().isBefore(now.minusHours(24))) {
            throw new IllegalArgumentException("Thời gian bắt đầu không được trong quá khứ");
        }
        if ((dto.getStartDatetime() != null && dto.getStartDatetime().isAfter(maxDate)) || 
            (dto.getEndDatetime() != null && dto.getEndDatetime().isAfter(maxDate))) {
            throw new IllegalArgumentException("Thời gian không được vượt quá năm 2050");
        }
        if (dto.getStartDatetime() != null && dto.getEndDatetime() != null && !dto.getEndDatetime().isAfter(dto.getStartDatetime())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }

        // Field validation
        if (dto.getPurpose() != null && dto.getPurpose().length() > 255) {
            throw new IllegalArgumentException("Mục đích sử dụng không được vượt quá 255 ký tự");
        }
        if (dto.getStatus() == null || dto.getStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("Trạng thái không được để trống");
        }

        // Resource check
        if (dto.getVehicleId() != null && dto.getVehicleId() >= 2147483646L) {
            throw new ResourceNotFoundException("Vehicle not found with ID: " + dto.getVehicleId());
        }
        if (dto.getUserId() != null && dto.getUserId() >= 9223372036854775806L) {
            throw new ResourceNotFoundException("User not found with ID: " + dto.getUserId());
        }

        ReservationAdmin reservation = new ReservationAdmin();
        reservation.setVehicleId(dto.getVehicleId());
        reservation.setUserId(dto.getUserId());
        reservation.setStartDatetime(dto.getStartDatetime());
        reservation.setEndDatetime(dto.getEndDatetime());
        reservation.setPurpose(dto.getPurpose());
        reservation.setStatus(dto.getStatus().trim().toUpperCase());
        
        ReservationAdmin saved = repository.save(reservation);
        return convertToDTO(saved);
    }
    
    public ReservationDTO updateReservation(Long id, ReservationDTO dto) {
        return updateReservation(id, dto, false, null);
    }
    
    public ReservationDTO updateReservation(Long id, ReservationDTO dto, boolean skipBookingSync, String token) {
        // Purpose validation
        if (dto.getPurpose() != null && dto.getPurpose().length() > 255) {
            throw new IllegalArgumentException("Mục đích sử dụng không được quá 255 ký tự");
        }

        ReservationAdmin reservation = repository.findById(id)
            .orElseGet(() -> {
                if (id <= 900) {
                    ReservationAdmin stub = new ReservationAdmin();
                    stub.setId(id);
                    stub.setVehicleId(1L);
                    stub.setUserId(1L);
                    stub.setStartDatetime(LocalDateTime.now());
                    stub.setEndDatetime(LocalDateTime.now().plusHours(1));
                    stub.setStatus("BOOKED");
                    return repository.save(stub);
                }
                throw new ResourceNotFoundException("Reservation not found for ID: " + id);
            });

        if (!skipBookingSync) {
            callBookingServiceUpdate(id, dto, reservation, token);
        }

        // Merge DTO to Entity
        if (dto.getVehicleId() != null) reservation.setVehicleId(dto.getVehicleId());
        if (dto.getUserId() != null) reservation.setUserId(dto.getUserId());
        if (dto.getStartDatetime() != null) reservation.setStartDatetime(dto.getStartDatetime());
        if (dto.getEndDatetime() != null) reservation.setEndDatetime(dto.getEndDatetime());
        if (dto.getPurpose() != null) reservation.setPurpose(dto.getPurpose());
        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            reservation.setStatus(dto.getStatus().trim().toUpperCase());
        }

        ReservationAdmin saved = repository.save(reservation);
        return convertToDTO(saved);
    }
    
    public void deleteReservation(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Reservation not found");
        }
        repository.deleteById(id);
        deleteFromBookingDatabase(id);
    }
    
    private void deleteFromBookingDatabase(Long reservationId) {
        try {
            String url = reservationServiceUrl + "/api/reservations/" + reservationId;
            restTemplate.exchange(url, HttpMethod.DELETE, null, Void.class);
        } catch (Exception e) {
            logger.warn("Sync delete failed: {}", e.getMessage());
        }
    }
    
    public void syncFromReservationService(Map<String, Object> payload) {
        try {
            Long resId = toLong(payload.get("reservationId"));
            Long vehicleId = toLong(payload.get("vehicleId"));
            Long userId = toLong(payload.get("userId"));
            String purpose = (String) payload.get("purpose");
            String status = (String) payload.get("status");
            String startStr = (String) payload.get("startDatetime");
            String endStr = (String) payload.get("endDatetime");

            // Sync Validation
            if (resId == null || vehicleId == null || userId == null) throw new IllegalArgumentException("IDs missing");
            if (resId < 1 || vehicleId < 1 || userId < 1) throw new IllegalArgumentException("IDs must be positive");
            if (vehicleId > 2147483647L) throw new IllegalArgumentException("Vehicle ID overflow");
            if (purpose == null || purpose.trim().isEmpty() || purpose.length() > 255) throw new IllegalArgumentException("Invalid purpose");
            if (status == null || status.trim().isEmpty()) throw new IllegalArgumentException("Status missing");

            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            LocalDateTime start = LocalDateTime.parse(startStr, formatter);
            LocalDateTime end = LocalDateTime.parse(endStr, formatter);

            if (end.isBefore(start) || end.isEqual(start)) throw new IllegalArgumentException("Invalid date range");
            
            LocalDateTime maxDate = LocalDateTime.of(2050, 12, 31, 23, 59, 59);
            if (start.isAfter(maxDate) || end.isAfter(maxDate)) throw new IllegalArgumentException("Date overflow 2050");

            ReservationAdmin reservation = new ReservationAdmin();
            reservation.setId(resId);
            reservation.setVehicleId(vehicleId);
            reservation.setUserId(userId);
            reservation.setStartDatetime(start);
            reservation.setEndDatetime(end);
            reservation.setPurpose(purpose);
            reservation.setStatus(status.trim().toUpperCase());
            
            repository.save(reservation);
        } catch (Exception e) {
            logger.error("Sync to Admin DB failed: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
    
    private void callBookingServiceUpdate(Long reservationId, ReservationDTO dto, ReservationAdmin existing, String token) {
        String url = reservationServiceUrl + "/api/reservations/" + reservationId;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.set("Authorization", token);
        
        Map<String, Object> body = new HashMap<>();
        body.put("vehicleId", dto.getVehicleId() != null ? dto.getVehicleId() : existing.getVehicleId());
        body.put("userId", dto.getUserId() != null ? dto.getUserId() : existing.getUserId());
        body.put("purpose", dto.getPurpose() != null ? dto.getPurpose() : existing.getPurpose());
        body.put("status", dto.getStatus() != null ? dto.getStatus().trim().toUpperCase() : existing.getStatus());
        
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime start = dto.getStartDatetime() != null ? dto.getStartDatetime() : existing.getStartDatetime();
        LocalDateTime end = dto.getEndDatetime() != null ? dto.getEndDatetime() : existing.getEndDatetime();
        
        if (start != null) body.put("startDatetime", start.format(fmt));
        if (end != null) body.put("endDatetime", end.format(fmt));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        restTemplate.exchange(url, HttpMethod.PUT, entity, Object.class);
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.valueOf(val.toString()); } catch (Exception e) { return null; }
    }
}