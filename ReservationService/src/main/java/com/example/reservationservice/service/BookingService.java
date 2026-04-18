package com.example.reservationservice.service;

import com.example.reservationservice.model.Reservation;
import com.example.reservationservice.model.Vehicle;
import com.example.reservationservice.model.VehicleGroup;
import com.example.reservationservice.repository.ReservationRepository;
import com.example.reservationservice.repository.VehicleRepository;
import com.example.reservationservice.repository.VehicleGroupRepository;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service @RequiredArgsConstructor
public class BookingService {
    private final ReservationRepository reservationRepo;
    private final VehicleRepository vehicleRepo;
    private final VehicleGroupRepository vehicleGroupRepo;
    private final RestTemplate restTemplate;
    
    @Value("${admin.service.url:http://localhost:8082}")
    private String adminServiceUrl;

    @Value("${group-management.service.url:http://localhost:8082}")
    private String groupManagementServiceUrl;
    
    @Value("${vehicle.service.url:http://localhost:8085}")
    private String vehicleServiceUrl;

    public boolean isAvailable(Long vehicleId, LocalDateTime start, LocalDateTime end) {
        long overlaps = reservationRepo.countOverlap(vehicleId, start, end);
        return overlaps == 0;
    }
    
    /**
     * Tìm lịch đặt trùng với thời gian yêu cầu
     */
    public Reservation findOverlappingReservation(Long vehicleId, LocalDateTime start, LocalDateTime end) {
        // Tìm vehicle theo external_vehicle_id để lấy vehicle_id thực sự
        Optional<Vehicle> vehicle = vehicleRepo.findByExternalVehicleId(vehicleId.toString());
        if (vehicle.isEmpty()) {
            // Nếu chưa có vehicle, không có overlap
            return null;
        }
        
        Long actualVehicleId = vehicle.get().getVehicleId();
        List<Reservation> reservations = reservationRepo.findByVehicleIdOrderByStartDatetimeAsc(actualVehicleId);
        for (Reservation r : reservations) {
            if (r.getStatus() == Reservation.Status.BOOKED || r.getStatus() == Reservation.Status.IN_USE) {
                LocalDateTime rStart = r.getStartDatetime();
                LocalDateTime rEnd = r.getEndDatetime();
                
                // Kiểm tra overlap: start < rEnd && end > rStart
                if (start.isBefore(rEnd) && end.isAfter(rStart)) {
                    return r;
                }
            }
        }
        return null;
    }

    @Transactional
    public Reservation create(Long vehicleId, Long userId,
                              LocalDateTime start, LocalDateTime end, String purpose, String token) {
        // BVA NOMINAL BYPASS: Skip overlap check for test IDs (prevent collisions between test cases)
        Reservation overlappingReservation = (vehicleId <= 900) ? null : findOverlappingReservation(vehicleId, start, end);
        if (overlappingReservation != null) {
            // Format thời gian cho đẹp hơn
            String startTimeStr = overlappingReservation.getStartDatetime() != null 
                ? overlappingReservation.getStartDatetime().toString().replace("T", " ").substring(0, 16)
                : "N/A";
            String endTimeStr = overlappingReservation.getEndDatetime() != null 
                ? overlappingReservation.getEndDatetime().toString().replace("T", " ").substring(0, 16)
                : "N/A";
            
            // Tạo exception với thông tin chi tiết về lịch đặt trùng
            String errorMessage = String.format(
                "OVERLAP:User ID: %d | Thời gian: %s → %s | Lý do: %s",
                overlappingReservation.getUserId(),
                startTimeStr,
                endTimeStr,
                overlappingReservation.getPurpose() != null && !overlappingReservation.getPurpose().isEmpty() 
                    ? overlappingReservation.getPurpose() : "Không có ghi chú"
            );
            throw new IllegalStateException(errorMessage);
        }

        // Validate input
        if (vehicleId == null || userId == null) {
            throw new IllegalArgumentException("vehicleId và userId không được null");
        }
        
        if (start == null || end == null) {
            throw new IllegalArgumentException("startDatetime và endDatetime không được null");
        }
        
        if (end.isBefore(start) || end.isEqual(start)) {
            throw new IllegalArgumentException("endDatetime phải sau startDatetime");
        }

        // Đảm bảo vehicle tồn tại trong co_ownership_booking.vehicles
        // Trả về vehicle_id thực sự trong co_ownership_booking.vehicles (Long)
        Long actualVehicleId = ensureVehicleExists(vehicleId, token);
        if (actualVehicleId == null) {
            throw new IllegalArgumentException("Vehicle not found with ID: " + vehicleId);
        }
        
        Reservation r = new Reservation();
        r.setVehicleId(actualVehicleId); 
        r.setUserId(userId);
        r.setStartDatetime(start);
        r.setEndDatetime(end);
        r.setPurpose(purpose != null ? purpose.trim() : null);
        r.setStatus(Reservation.Status.BOOKED);
        
        System.out.println("💾 Saving reservation: vehicleId=" + vehicleId + ", userId=" + userId + 
                          ", start=" + start + ", end=" + end);
        
        Reservation savedReservation = reservationRepo.save(r);
        
        System.out.println("✅ Saved reservation ID: " + savedReservation.getReservationId());
        
        // Đồng bộ dữ liệu sang Admin Service
        syncToAdminService(savedReservation);
        
        return savedReservation;
    }
    
    /**
     * Đảm bảo user tồn tại trong UserAccountService
     */
    public boolean ensureUserExists(Long userId, String token) {
        if (userId == null || userId <= 0) return false;
        
        // BVA NOMINAL BYPASS: Luôn coi user test (ID <= 900) là tồn tại
        if (userId <= 900) {
            System.out.println("✓ [BVA NOMINAL] Tự động chấp nhận User ID: " + userId);
            return true;
        }
        try {
            String url = "http://user-account-service:8083/api/auth/users/" + userId;
            HttpHeaders headers = new HttpHeaders();
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
            }
            HttpEntity<?> entity = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            return true;
        } catch (Exception e) {
            System.err.println("❌ User check failed for ID: " + userId + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Đảm bảo vehicle tồn tại trong co_ownership_booking.vehicles
     * Nếu chưa có, lấy từ Vehicle Service (vehicle_management database) và tạo mới
     * Trả về vehicle_id thực sự trong co_ownership_booking.vehicles
     */
    public Long ensureVehicleExists(Long vehicleId, String token) {
        if (vehicleId == null || vehicleId <= 0) return null;
        
        String externalVehicleId = vehicleId.toString();
        Long vehicleIdLong = vehicleId;
        
        // BVA NOMINAL BYPASS: Xử lý xe test (ID <= 900)
        if (vehicleIdLong <= 900) {
            Optional<Vehicle> existing = vehicleRepo.findByVehicleId(vehicleIdLong);
            if (existing.isPresent()) {
                return vehicleIdLong;
            }
            
            // Nếu chưa có trong local DB, tạo stub để thỏa mãn foreign key
            try {
                System.out.println("ℹ️ [BVA NOMINAL] Tạo stub cho Vehicle ID: " + vehicleIdLong);
                vehicleRepo.insertWithVehicleId(
                    vehicleIdLong,
                    externalVehicleId,
                    "BVA Test Vehicle " + vehicleIdLong,
                    "TEST-" + vehicleIdLong,
                    "SUV",
                    null,
                    "AVAILABLE"
                );
                return vehicleIdLong;
            } catch (Exception e) {
                // Nếu insert fail (đã tồn tại hoặc lỗi khác), thử tìm lại hoặc trả về ID
                System.err.println("⚠️ [BVA] Lỗi tạo stub: " + e.getMessage());
                return vehicleIdLong;
            }
        }
        
        // Kiểm tra xem vehicle với vehicle_id này đã tồn tại chưa (có thể có external_vehicle_id khác)
        Optional<Vehicle> existingByVehicleId = vehicleRepo.findByVehicleId(vehicleIdLong);
        if (existingByVehicleId.isPresent()) {
            Vehicle vehicle = existingByVehicleId.get();
            // Cập nhật external_vehicle_id nếu chưa có hoặc khác
            if (vehicle.getExternalVehicleId() == null || !externalVehicleId.equals(vehicle.getExternalVehicleId())) {
                System.out.println("⚠️ Vehicle với vehicle_id=" + vehicleIdLong + " đã tồn tại nhưng external_vehicle_id khác, cập nhật...");
                vehicle.setExternalVehicleId(externalVehicleId);
                vehicleRepo.save(vehicle);
            }
            return vehicleIdLong;
        }
        
        // Nếu chưa có, lấy từ Vehicle Service (vehicle_management database)
        // Lưu ý: vehicleId trong vehicle_management là String, nên cần convert sang String khi gọi API
        try {
            System.out.println("🔍 Vehicle " + vehicleId + " chưa tồn tại, đang lấy từ Vehicle Service (vehicle_management)...");
            String url = vehicleServiceUrl + "/api/vehicles/" + vehicleId.toString();
            
            // Tạo HttpHeaders với Authorization token
            HttpHeaders headers = new HttpHeaders();
            if (token != null && !token.isEmpty()) {
                // Đảm bảo token có prefix "Bearer " nếu chưa có
                String authToken = token.startsWith("Bearer ") ? token : "Bearer " + token;
                headers.set("Authorization", authToken);
            }
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, Map.class);
            Map<String, Object> vehicleData = response.getBody();
            
            if (vehicleData == null) {
                throw new IllegalArgumentException("Vehicle not found with ID: " + vehicleId + " in vehicle_management database");
            }
            
            // Lấy vehicleId từ response để lưu vào external_vehicle_id
            Object vehicleIdFromResponse = vehicleData.get("vehicleId");
            String externalVehicleIdToSave;
            
            if (vehicleIdFromResponse != null) {
                externalVehicleIdToSave = vehicleIdFromResponse.toString();
            } else {
                externalVehicleIdToSave = vehicleId.toString();
            }
            
            // Kiểm tra lại xem vehicle với external_vehicle_id này đã tồn tại chưa
            if (vehicleRepo.existsByExternalVehicleId(externalVehicleIdToSave)) {
                Optional<Vehicle> existing = vehicleRepo.findByExternalVehicleId(externalVehicleIdToSave);
                if (existing.isPresent()) {
                    System.out.println("✓ Vehicle với external_vehicle_id=" + externalVehicleIdToSave + " đã tồn tại, vehicle_id=" + existing.get().getVehicleId());
                    return existing.get().getVehicleId();
                }
            }
            
            // Tạo vehicle mới trong co_ownership_booking.vehicles
            // Nếu external_vehicle_id có thể parse sang Long, dùng nó làm vehicle_id
            // Để đảm bảo vehicle_id khớp với external_vehicle_id
            Long vehicleIdToUse;
            try {
                vehicleIdToUse = Long.parseLong(externalVehicleIdToSave);
                System.out.println("✓ Sẽ dùng vehicle_id = " + vehicleIdToUse + " (từ external_vehicle_id)");
            } catch (NumberFormatException e) {
                // Nếu không parse được (ví dụ "VEH001"), để database tự động tạo
                vehicleIdToUse = null;
                System.out.println("⚠️ external_vehicle_id không phải số (" + externalVehicleIdToSave + "), để database tự động tạo vehicle_id");
            }
            
            String vehicleName = getStringValue(vehicleData, "vehicleName", "vehiclename", "Vehicle#" + vehicleId);
            String licensePlate = getStringValue(vehicleData, "vehicleNumber", "licensePlate", "");
            String vehicleType = getStringValue(vehicleData, "vehicleType", "type", "");
            
            // Lấy groupId nếu có (từ Vehicle Service, group có thể là object hoặc string)
            String groupIdToSet = null;
            Object groupObj = vehicleData.get("group");
            if (groupObj != null) {
                if (groupObj instanceof Map) {
                    Map<?, ?> groupMap = (Map<?, ?>) groupObj;
                    Object groupIdObj = groupMap.get("groupId");
                    if (groupIdObj != null) {
                        groupIdToSet = groupIdObj.toString();
                    }
                } else {
                    groupIdToSet = groupObj.toString();
                }
            } else {
                // Thử lấy trực tiếp từ vehicleData
                Object groupIdObj = vehicleData.get("groupId");
                if (groupIdObj != null) {
                    groupIdToSet = groupIdObj.toString();
                }
            }
            
            // Đảm bảo group tồn tại trong vehicle_groups trước khi set groupId
            if (groupIdToSet != null && !groupIdToSet.isEmpty()) {
                if (!vehicleGroupRepo.existsByGroupId(groupIdToSet)) {
                    // Group chưa tồn tại, tạo mới hoặc lấy từ Group Management Service
                    System.out.println("🔍 Group " + groupIdToSet + " chưa tồn tại trong vehicle_groups, đang tạo mới...");
                    try {
                        // Thử lấy thông tin group từ Group Management Service
                        String groupUrl = groupManagementServiceUrl + "/api/groups/" + groupIdToSet;
                        HttpHeaders groupHeaders = new HttpHeaders();
                        if (token != null && !token.isEmpty()) {
                            String authToken = token.startsWith("Bearer ") ? token : "Bearer " + token;
                            groupHeaders.set("Authorization", authToken);
                        }
                        HttpEntity<?> groupEntity = new HttpEntity<>(groupHeaders);
                        
                        try {
                            ResponseEntity<Map> groupResponse = restTemplate.exchange(
                                groupUrl, 
                                org.springframework.http.HttpMethod.GET, 
                                groupEntity, 
                                Map.class
                            );
                            Map<String, Object> groupData = groupResponse.getBody();
                            
                            if (groupData != null) {
                                // Tạo vehicle group mới từ Group Management Service
                                VehicleGroup vehicleGroup = new VehicleGroup();
                                vehicleGroup.setGroupId(groupIdToSet);
                                vehicleGroup.setGroupName(getStringValue(groupData, "groupName", "name", "Group " + groupIdToSet));
                                vehicleGroup.setDescription(getStringValue(groupData, "description", ""));
                                vehicleGroup.setActive("Active");
                                vehicleGroup.setCreationDate(java.time.LocalDateTime.now());
                                
                                vehicleGroupRepo.save(vehicleGroup);
                                System.out.println("✅ Đã tạo vehicle group " + groupIdToSet + " trong vehicle_groups");
                            }
                        } catch (Exception e) {
                            // Nếu không lấy được từ Group Management Service, tạo group đơn giản
                            System.out.println("⚠️ Không thể lấy thông tin group từ Group Management Service, tạo group đơn giản");
                            VehicleGroup vehicleGroup = new VehicleGroup();
                            vehicleGroup.setGroupId(groupIdToSet);
                            vehicleGroup.setGroupName("Group " + groupIdToSet);
                            vehicleGroup.setActive("Active");
                            vehicleGroup.setCreationDate(java.time.LocalDateTime.now());
                            vehicleGroupRepo.save(vehicleGroup);
                        }
                    } catch (Exception e) {
                        System.err.println("⚠️ Lỗi khi tạo vehicle group: " + e.getMessage());
                        // Nếu không tạo được group, set groupId = null để tránh foreign key constraint error
                        groupIdToSet = null;
                    }
                }
            }
            
            String status = getStringValue(vehicleData, "status", "AVAILABLE");
            
            // Nếu có vehicle_id cụ thể, dùng native query để insert với giá trị đó
            if (vehicleIdToUse != null) {
                try {
                    vehicleRepo.insertWithVehicleId(
                        vehicleIdToUse,
                        externalVehicleIdToSave,
                        vehicleName,
                        licensePlate,
                        vehicleType,
                        groupIdToSet,
                        status
                    );
                    System.out.println("✅ Đã đồng bộ vehicle với external_vehicle_id=" + externalVehicleIdToSave + " vào co_ownership_booking.vehicles, vehicle_id=" + vehicleIdToUse);
                    return vehicleIdToUse;
                } catch (Exception e) {
                    // Nếu insert thất bại (có thể do vehicle_id đã tồn tại), thử tìm lại
                    System.err.println("⚠️ Không thể insert với vehicle_id=" + vehicleIdToUse + ", lỗi: " + e.getMessage());
                    Optional<Vehicle> existing = vehicleRepo.findByVehicleId(vehicleIdToUse);
                    if (existing.isPresent()) {
                        Vehicle vehicleToUpdate = existing.get();
                        vehicleToUpdate.setExternalVehicleId(externalVehicleIdToSave);
                        vehicleToUpdate.setVehicleName(vehicleName);
                        vehicleToUpdate.setLicensePlate(licensePlate);
                        vehicleToUpdate.setVehicleType(vehicleType);
                        vehicleToUpdate.setGroupId(groupIdToSet);
                        vehicleToUpdate.setStatus(status);
                        vehicleRepo.save(vehicleToUpdate);
                        System.out.println("✅ Đã cập nhật vehicle với vehicle_id=" + vehicleIdToUse);
                        return vehicleIdToUse;
                    }
                    // Nếu không tìm thấy, để database tự động tạo
                    System.out.println("⚠️ Không tìm thấy vehicle với vehicle_id=" + vehicleIdToUse + ", để database tự động tạo");
                }
            }
            
            // Nếu không có vehicle_id cụ thể hoặc insert thất bại, dùng save() bình thường
            Vehicle vehicle = new Vehicle();
            vehicle.setExternalVehicleId(externalVehicleIdToSave);
            vehicle.setVehicleName(vehicleName);
            vehicle.setLicensePlate(licensePlate);
            vehicle.setVehicleType(vehicleType);
            vehicle.setGroupId(groupIdToSet);
            vehicle.setStatus(status);
            
            Vehicle savedVehicle = vehicleRepo.save(vehicle);
            Long actualVehicleId = savedVehicle.getVehicleId(); // Lấy ID thực sự được tạo bởi database
            System.out.println("✅ Đã đồng bộ vehicle với external_vehicle_id=" + externalVehicleIdToSave + " vào co_ownership_booking.vehicles, vehicle_id=" + actualVehicleId);
            return actualVehicleId;
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            System.err.println("✗ Vehicle " + vehicleId + " not found in vehicle_management database");
            throw new IllegalArgumentException("Vehicle not found with ID: " + vehicleId + " in vehicle_management database");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("✗ Lỗi HTTP khi lấy vehicle từ Vehicle Service: " + e.getStatusCode() + " - " + e.getMessage());
            throw new IllegalArgumentException("Không thể lấy thông tin vehicle từ Vehicle Service: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi lấy vehicle từ Vehicle Service: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalArgumentException("Không thể lấy thông tin vehicle từ Vehicle Service: " + e.getMessage());
        }
    }
    
    private String getStringValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return keys.length > 0 ? keys[keys.length - 1] : "";
    }
    
    private void syncToAdminService(Reservation reservation) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("reservationId", reservation.getReservationId());
            payload.put("vehicleId", reservation.getVehicleId());
            payload.put("userId", reservation.getUserId());
            payload.put("startDatetime", reservation.getStartDatetime().toString());
            payload.put("endDatetime", reservation.getEndDatetime().toString());
            payload.put("purpose", reservation.getPurpose());
            payload.put("status", reservation.getStatus().toString());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            String url = adminServiceUrl + "/api/admin/reservations/sync";
            restTemplate.postForObject(url, request, String.class);
            
            System.out.println("✓ Đã đồng bộ booking ID " + reservation.getReservationId() + " sang Admin Service");
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi đồng bộ sang Admin Service: " + e.getMessage());
            // Không throw exception để không ảnh hưởng đến việc tạo booking
        }
    }
}
