package com.example.reservationservice.service;

import com.example.reservationservice.dto.*;
<<<<<<< HEAD
import com.example.reservationservice.exception.UnauthorizedException;
=======
>>>>>>> origin/main
import com.example.reservationservice.model.Reservation;
import com.example.reservationservice.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FairnessEngineService {

    private static final double DIFFERENCE_THRESHOLD = 5.0;

    private final ReservationRepository reservationRepository;
    private final GroupManagementApiService groupManagementApiService;
    
    @Value("${user-account.service.url:http://localhost:8083}")
    private String userAccountServiceUrl;
    
    @Value("${vehicle.service.url:http://localhost:8084}")
    private String vehicleServiceUrl;

    @Transactional(readOnly = true)
<<<<<<< HEAD
    public FairnessSummaryDTO buildSummary(Long vehicleId, Integer rangeDays) {
=======
    public FairnessSummaryDTO buildSummary(Integer vehicleId, Integer rangeDays) {
>>>>>>> origin/main
        return buildSummary(vehicleId, rangeDays, null);
    }
    
    @Transactional(readOnly = true)
<<<<<<< HEAD
    public FairnessSummaryDTO buildSummary(Long vehicleId, Integer rangeDays, String token) {
=======
    public FairnessSummaryDTO buildSummary(Integer vehicleId, Integer rangeDays, String token) {
>>>>>>> origin/main
        // Lấy thông tin group từ vehicleId
        Optional<Map<String, Object>> groupOpt = groupManagementApiService.getGroupByVehicleId(vehicleId, token);
        if (groupOpt.isEmpty()) {
            throw new IllegalArgumentException("Vehicle not found or not associated with any group");
        }
        
        Map<String, Object> group = groupOpt.get();
<<<<<<< HEAD
        Long groupId = group.get("groupId") instanceof Number 
            ? ((Number) group.get("groupId")).longValue() 
            : Long.parseLong(group.get("groupId").toString());
=======
        Integer groupId = (Integer) group.get("groupId");
>>>>>>> origin/main
        
        // Lấy danh sách members từ group-management-service
        List<Map<String, Object>> membersData = groupManagementApiService.getGroupMembers(groupId, token);
        
        // Lấy thông tin vehicle (tên xe) - có thể cần gọi vehicle-service
        String vehicleName = getVehicleName(vehicleId);

        int days = rangeDays != null && rangeDays > 0 ? rangeDays : 30;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime rangeStart = now.minusDays(days);
        LocalDateTime rangeEnd = now.plusDays(days);

        List<Reservation> reservations = reservationRepository
                .findByVehicleAndRange(vehicleId, rangeStart, rangeEnd);

<<<<<<< HEAD
        Map<Long, Double> usageHours = new HashMap<>();
        Map<Long, LocalDateTime> lastUsage = new HashMap<>();
        Map<Long, LocalDateTime> nextUsage = new HashMap<>();
=======
        Map<Integer, Double> usageHours = new HashMap<>();
        Map<Integer, LocalDateTime> lastUsage = new HashMap<>();
        Map<Integer, LocalDateTime> nextUsage = new HashMap<>();
>>>>>>> origin/main

        reservations.stream()
                .filter(r -> r.getStartDatetime() != null && r.getEndDatetime() != null)
                .forEach(reservation -> {
                    double hours = calculateDurationHours(reservation.getStartDatetime(), reservation.getEndDatetime());
<<<<<<< HEAD
                    Long userId = reservation.getUserId();
=======
                    Integer userId = reservation.getUserId();
>>>>>>> origin/main
                    usageHours.merge(userId, hours, Double::sum);

                    if (reservation.getEndDatetime().isBefore(now)) {
                        lastUsage.compute(userId, (k, v) -> {
                            if (v == null || reservation.getEndDatetime().isAfter(v)) {
                                return reservation.getEndDatetime();
                            }
                            return v;
                        });
                    } else if (reservation.getStartDatetime().isAfter(now)) {
                        nextUsage.compute(userId, (k, v) -> {
                            if (v == null || reservation.getStartDatetime().isBefore(v)) {
                                return reservation.getStartDatetime();
                            }
                            return v;
                        });
                    }
                });

        double totalUsageHours = usageHours.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        List<FairnessReservationDTO> reservationDTOs = reservations.stream()
                .map(this::mapReservation)
                .collect(Collectors.toList());

        List<FairnessMemberDTO> memberStats = membersData.stream()
                .map(memberData -> buildMemberStats(memberData, usageHours, totalUsageHours, lastUsage, nextUsage))
                .sorted(Comparator.comparingDouble(FairnessMemberDTO::getDifference))
                .collect(Collectors.toList());

<<<<<<< HEAD
        List<Long> priorityQueue = memberStats.stream()
=======
        List<Integer> priorityQueue = memberStats.stream()
>>>>>>> origin/main
                .sorted(Comparator
                        .comparing(FairnessMemberDTO::getPriority, this::priorityCompare)
                        .thenComparingDouble(FairnessMemberDTO::getDifference))
                .map(FairnessMemberDTO::getUserId)
                .collect(Collectors.toList());

        double fairnessIndex = memberStats.stream()
                .mapToDouble(FairnessMemberDTO::getFairnessScore)
                .average()
                .orElse(100.0);

        List<FairnessAvailabilityDTO> availabilitySlots = buildAvailabilityWindows(rangeStart, rangeEnd, reservationDTOs);

        return FairnessSummaryDTO.builder()
                .vehicleId(vehicleId)
                .vehicleName(vehicleName)
                .groupId(groupId)
                .groupName("Group#" + groupId) // Hiển thị Group ID thay vì tên để tránh lỗi encoding
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .generatedAt(LocalDateTime.now())
                .totalUsageHours(totalUsageHours)
                .fairnessIndex(roundTwoDecimals(fairnessIndex))
                .members(memberStats)
                .reservations(reservationDTOs)
                .availability(availabilitySlots)
                .priorityQueue(priorityQueue)
                .build();
    }

    @Transactional(readOnly = true)
<<<<<<< HEAD
    public FairnessSuggestionResponse suggest(Long vehicleId, FairnessSuggestionRequest request, String token) {
        // [LOG] Debug Token: token is null? (token == null)
        System.out.println("[CRITICAL_LOG] Receiving suggest request for vehicle " + vehicleId + " with token prefix: " + (token != null && token.length() > 10 ? token.substring(0, 10) : token));

        // FS_26, 27, 28: Yêu cầu Header Authorization bắt đầu bảng 'Bearer ' (401)
        if (token == null || token.trim().isEmpty() || token.length() < 10 || !token.startsWith("Bearer ")) {
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }

        // FS_07: Chặn VehicleId vượt giới hạn (Bán lại: Không còn chặn Integer.MAX_VALUE vì đã dùng Long)
        if (vehicleId != null && vehicleId <= 0) {
            throw new IllegalArgumentException("Invalid vehicleId: ID must be positive");
        }

        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        
        // FS_08: Chặn UserId không hợp lệ (BVA: 0 hoặc âm -> 400)
        if (request.getUserId() <= 0) {
            throw new IllegalArgumentException("Invalid userId: ID must be positive");
        }

        // FS_20, 25: Chặn thời lượng không hợp lệ (BVA: <= 0 hoặc > 24 -> 400)
        if (request.getDurationHours() != null) {
            if (request.getDurationHours() <= 0) {
                throw new IllegalArgumentException("Invalid duration: duration must be greater than zero");
            }
            if (request.getDurationHours() > 24.0) {
                throw new IllegalArgumentException("Duration cannot exceed 24 hours");
            }
        }

        // FS_14, 15, 16: Chặn đặt lịch trong quá khứ (Nới lỏng 12 tiếng để bao phủ mọi sai lệch múi giờ giữa Postman/Server)
        if (request.getDesiredStart() != null && request.getDesiredStart().isBefore(LocalDateTime.now().minusHours(12))) {
            throw new IllegalArgumentException("Thời gian bắt đầu mong muốn không được nằm trong quá khứ xa hơn 12 tiếng");
        }

        FairnessSummaryDTO summary = buildSummary(vehicleId, 30, token);
=======
    public FairnessSuggestionResponse suggest(Integer vehicleId, FairnessSuggestionRequest request) {
        return suggest(vehicleId, request, null);
    }
    
    @Transactional(readOnly = true)
    public FairnessSuggestionResponse suggest(Integer vehicleId, FairnessSuggestionRequest request, String token) {
        FairnessSummaryDTO summary = buildSummary(vehicleId, 30, token);
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
>>>>>>> origin/main

        FairnessMemberDTO applicant = summary.getMembers().stream()
                .filter(m -> Objects.equals(m.getUserId(), request.getUserId()))
                .findFirst()
<<<<<<< HEAD
                .orElseThrow(() -> new IllegalArgumentException("User not found in vehicle's co-ownership list"));
=======
                .orElseThrow(() -> new IllegalArgumentException("User is not in vehicle's co-ownership list"));
>>>>>>> origin/main

        LocalDateTime desiredStart = Optional.ofNullable(request.getDesiredStart())
                .orElse(LocalDateTime.now().plusHours(1));
        LocalDateTime desiredEnd = desiredStart.plusMinutes(
                Math.max(1L, Math.round(Optional.ofNullable(request.getDurationHours()).orElse(2.0) * 60))
        );

        List<FairnessReservationDTO> conflicts = summary.getReservations().stream()
                .filter(r -> !Objects.equals(r.getStatus(), Reservation.Status.CANCELLED.name()))
                .filter(r -> overlaps(desiredStart, desiredEnd, r.getStart(), r.getEnd()))
                .collect(Collectors.toList());

        boolean otherMembersNeedPriority = summary.getMembers().stream()
                .filter(m -> !Objects.equals(m.getUserId(), applicant.getUserId()))
                .anyMatch(m -> "HIGH".equalsIgnoreCase(m.getPriority()));

        boolean approved = !"LOW".equalsIgnoreCase(applicant.getPriority())
                || (!otherMembersNeedPriority && conflicts.isEmpty());

        String reason;
        if (!approved && "LOW".equalsIgnoreCase(applicant.getPriority())) {
            reason = "Thành viên khác đang được ưu tiên hơn (chênh lệch sử dụng lớn).";
        } else if (!conflicts.isEmpty()) {
            reason = "Thời gian yêu cầu đang bị trùng với lịch khác.";
        } else {
            reason = applicant.getPriority().equalsIgnoreCase("HIGH")
                    ? "Bạn đang được ưu tiên do sử dụng ít hơn quyền sở hữu."
                    : "Bạn có thể đặt lịch trong khung giờ mong muốn.";
        }

        List<FairnessAvailabilityDTO> recommendations = conflicts.isEmpty()
                ? List.of(FairnessAvailabilityDTO.builder()
                .start(desiredStart)
                .end(desiredEnd)
                .durationHours(roundTwoDecimals(calculateDurationHours(desiredStart, desiredEnd)))
                .label("Khung giờ đề xuất")
                .build())
                : findReplacementSlots(summary.getAvailability(), desiredStart, desiredEnd);

        return FairnessSuggestionResponse.builder()
                .vehicleId(vehicleId)
                .userId(request.getUserId())
                .approved(approved && conflicts.isEmpty())
                .priority(applicant.getPriority())
                .reason(reason)
                .requestedStart(desiredStart)
                .requestedEnd(desiredEnd)
                .applicant(applicant)
                .conflicts(conflicts)
                .recommendations(recommendations)
                .build();
    }

<<<<<<< HEAD
    private String getVehicleName(Long vehicleId) {
=======
    private String getVehicleName(Integer vehicleId) {
>>>>>>> origin/main
        // Tạm thời trả về ID, có thể gọi vehicle-service sau
        try {
            // Có thể gọi API từ vehicle-service nếu có
            // String url = vehicleServiceUrl + "/api/vehicles/" + vehicleId;
            // ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(url, ...);
            // return response.getBody().get("vehicleName");
            return "Vehicle " + vehicleId;
        } catch (Exception e) {
            return "Vehicle " + vehicleId;
        }
    }
    
<<<<<<< HEAD
    private String getUserName(Long userId) {
=======
    private String getUserName(Integer userId) {
>>>>>>> origin/main
        // Hiển thị User#n để tránh lỗi encoding tiếng Việt
        return "User#" + userId;
    }

    private double calculateDurationHours(LocalDateTime start, LocalDateTime end) {
        return Math.max(0.25, Duration.between(start, end).toMinutes() / 60.0);
    }

    private FairnessReservationDTO mapReservation(Reservation reservation) {
        return FairnessReservationDTO.builder()
                .reservationId(reservation.getReservationId())
                .vehicleId(reservation.getVehicleId())
                .vehicleName(getVehicleName(reservation.getVehicleId()))
                .userId(reservation.getUserId())
                .userName(getUserName(reservation.getUserId()))
                .start(reservation.getStartDatetime())
                .end(reservation.getEndDatetime())
                .status(reservation.getStatus().name())
                .purpose(reservation.getPurpose() != null ? reservation.getPurpose() : "")
                .build();
    }

    private FairnessMemberDTO buildMemberStats(Map<String, Object> memberData,
<<<<<<< HEAD
                                               Map<Long, Double> usageHours,
                                               double totalUsageHours,
                                               Map<Long, LocalDateTime> lastUsage,
                                               Map<Long, LocalDateTime> nextUsage) {
        Long userId = ((Number) memberData.get("userId")).longValue();
=======
                                               Map<Integer, Double> usageHours,
                                               double totalUsageHours,
                                               Map<Integer, LocalDateTime> lastUsage,
                                               Map<Integer, LocalDateTime> nextUsage) {
        Integer userId = ((Number) memberData.get("userId")).intValue();
>>>>>>> origin/main
        double hours = usageHours.getOrDefault(userId, 0.0);
        double usagePercentage = totalUsageHours > 0 ? (hours / totalUsageHours) * 100 : 0.0;
        
        Object ownershipObj = memberData.get("ownershipPercent");
        double ownership = ownershipObj != null ? ((Number) ownershipObj).doubleValue() : 0.0;
        
        double difference = usagePercentage - ownership;
        double fairnessScore = Math.max(0.0, 100.0 - Math.abs(difference) * 2.0);

        // Luôn hiển thị User#n để tránh lỗi encoding tiếng Việt
        String fullName = "User#" + userId;
        
        return FairnessMemberDTO.builder()
                .userId(userId)
                .fullName(fullName)
                .email((String) memberData.getOrDefault("email", ""))
                .ownershipPercentage(roundTwoDecimals(ownership))
                .usageHours(roundTwoDecimals(hours))
                .usagePercentage(roundTwoDecimals(usagePercentage))
                .difference(roundTwoDecimals(difference))
                .fairnessScore(roundTwoDecimals(fairnessScore))
                .priority(classifyPriority(difference))
                .lastUsageEnd(lastUsage.get(userId))
                .nextReservationStart(nextUsage.get(userId))
                .build();
    }

    private String classifyPriority(double difference) {
        if (difference <= -DIFFERENCE_THRESHOLD) {
            return "HIGH";
        }
        if (difference >= DIFFERENCE_THRESHOLD) {
            return "LOW";
        }
        return "NORMAL";
    }

    private int priorityCompare(String a, String b) {
        List<String> order = List.of("HIGH", "NORMAL", "LOW");
        return Integer.compare(order.indexOf(a.toUpperCase()), order.indexOf(b.toUpperCase()));
    }

    private List<FairnessAvailabilityDTO> buildAvailabilityWindows(LocalDateTime rangeStart,
                                                                   LocalDateTime rangeEnd,
                                                                   List<FairnessReservationDTO> reservations) {
        List<FairnessAvailabilityDTO> slots = new ArrayList<>();

        List<FairnessReservationDTO> sorted = reservations.stream()
                .sorted(Comparator.comparing(FairnessReservationDTO::getStart))
                .collect(Collectors.toList());

        LocalDateTime cursor = rangeStart;
        for (FairnessReservationDTO reservation : sorted) {
            if (reservation.getStart() == null || reservation.getEnd() == null) {
                continue;
            }
            if (cursor.isBefore(reservation.getStart())) {
                slots.add(buildSlot(cursor, reservation.getStart()));
            }
            if (cursor.isBefore(reservation.getEnd())) {
                cursor = reservation.getEnd();
            }
        }

        if (cursor.isBefore(rangeEnd)) {
            slots.add(buildSlot(cursor, rangeEnd));
        }

        return slots.stream()
                .filter(slot -> slot.getDurationHours() >= 0.25)
                .collect(Collectors.toList());
    }

    private FairnessAvailabilityDTO buildSlot(LocalDateTime start, LocalDateTime end) {
        double hours = roundTwoDecimals(calculateDurationHours(start, end));
        return FairnessAvailabilityDTO.builder()
                .start(start)
                .end(end)
                .durationHours(hours)
                .label(hours >= 1 ? String.format("Trống %.1f giờ", hours) : "Trống ngắn")
                .build();
    }

    private List<FairnessAvailabilityDTO> findReplacementSlots(List<FairnessAvailabilityDTO> availability,
                                                               LocalDateTime desiredStart,
                                                               LocalDateTime desiredEnd) {
        double requestedHours = calculateDurationHours(desiredStart, desiredEnd);
        return availability.stream()
                .filter(slot -> slot.getDurationHours() >= requestedHours)
                .sorted(Comparator.comparing(slot -> Duration.between(desiredStart, slot.getStart()).abs()))
                .limit(3)
                .collect(Collectors.toList());
    }

    private boolean overlaps(LocalDateTime start1, LocalDateTime end1,
                              LocalDateTime start2, LocalDateTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
