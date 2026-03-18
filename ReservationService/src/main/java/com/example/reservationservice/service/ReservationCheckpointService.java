package com.example.reservationservice.service;

import com.example.reservationservice.dto.CheckpointDTO;
import com.example.reservationservice.dto.CheckpointIssueRequest;
import com.example.reservationservice.dto.CheckpointScanRequest;
import com.example.reservationservice.dto.CheckpointSignRequest;
import com.example.reservationservice.model.Reservation;
import com.example.reservationservice.model.ReservationCheckpoint;
import com.example.reservationservice.model.ReservationCheckpoint.CheckpointStatus;
import com.example.reservationservice.model.ReservationCheckpoint.CheckpointType;
import com.example.reservationservice.repository.ReservationCheckpointRepository;
import com.example.reservationservice.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationCheckpointService {

    private final ReservationRepository reservationRepository;
    private final ReservationCheckpointRepository checkpointRepository;
    private final RestTemplate restTemplate;

    @Value("${admin.service.url:http://localhost:8082}")
    private String adminServiceUrl;

    @Transactional
    public CheckpointDTO issueCheckpoint(Long reservationId, CheckpointIssueRequest request) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        String typeValue = request.getType() != null ? request.getType() : "CHECK_IN";
        CheckpointType type = CheckpointType.valueOf(typeValue.toUpperCase());

        if (type == CheckpointType.CHECK_IN && !isWithinReservationWindow(reservation)) {
            System.out.println("⚠️ Check-in requested ngoài khoảng thời gian đặt. Vẫn cho phép phát QR cho mục đích thử nghiệm.");
        }

        // expire previous pending checkpoints for same type
        List<ReservationCheckpoint> actives = checkpointRepository
                .findActiveByReservationAndType(reservationId, type, List.of(
                        CheckpointStatus.PENDING, CheckpointStatus.SCANNED, CheckpointStatus.SIGNED
                ));
        actives.forEach(cp -> {
            cp.setStatus(CheckpointStatus.EXPIRED);
            cp.setExpiresAt(LocalDateTime.now());
        });
        checkpointRepository.saveAll(actives);

        ReservationCheckpoint checkpoint = new ReservationCheckpoint();
        checkpoint.setReservation(reservation);
        checkpoint.setCheckpointType(type);
        checkpoint.setStatus(CheckpointStatus.PENDING);
        checkpoint.setQrToken(UUID.randomUUID().toString());
        checkpoint.setIssuedBy(request.getIssuedBy() != null ? request.getIssuedBy() : "ADMIN");
        checkpoint.setNotes(request.getNotes());
        checkpoint.setExpiresAt(LocalDateTime.now().plusMinutes(
                request.getValidMinutes() != null ? Math.max(5, request.getValidMinutes()) : 15
        ));

        checkpointRepository.save(checkpoint);
        return toDto(checkpoint, true);
    }

    @Transactional
    public CheckpointDTO scanCheckpoint(CheckpointScanRequest request) {
        ReservationCheckpoint checkpoint = checkpointRepository.findByQrToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("QR token không hợp lệ"));

        if (checkpoint.getExpiresAt() != null && checkpoint.getExpiresAt().isBefore(LocalDateTime.now())) {
            checkpoint.setStatus(CheckpointStatus.EXPIRED);
            checkpointRepository.save(checkpoint);
            throw new IllegalStateException("QR đã hết hạn, vui lòng yêu cầu mã mới.");
        }

        checkpoint.setLatitude(request.getLatitude());
        checkpoint.setLongitude(request.getLongitude());
        checkpoint.setScannedAt(LocalDateTime.now());

        if (checkpoint.getStatus() == CheckpointStatus.PENDING) {
            checkpoint.setStatus(CheckpointStatus.SCANNED);
        }

        updateReservationUsageStatus(checkpoint.getReservation(), checkpoint.getCheckpointType());

        checkpointRepository.save(checkpoint);
        return toDto(checkpoint, false);
    }

    @Transactional
    public CheckpointDTO signCheckpoint(Long checkpointId, CheckpointSignRequest request) {
        ReservationCheckpoint checkpoint = checkpointRepository.findById(checkpointId)
                .orElseThrow(() -> new IllegalArgumentException("Checkpoint không tồn tại"));

        if (checkpoint.getStatus() == CheckpointStatus.EXPIRED) {
            throw new IllegalStateException("Checkpoint đã hết hạn.");
        }

        checkpoint.setSignerName(request.getSignerName());
        checkpoint.setSignerIdNumber(request.getSignerIdNumber());
        checkpoint.setSignatureData(request.getSignatureData());
        checkpoint.setSignedAt(LocalDateTime.now());
        checkpoint.setStatus(CheckpointStatus.COMPLETED);

        updateReservationUsageStatus(checkpoint.getReservation(), checkpoint.getCheckpointType());

        checkpointRepository.save(checkpoint);
        return toDto(checkpoint, false);
    }

    @Transactional(readOnly = true)
    public List<CheckpointDTO> getCheckpointsForReservation(Long reservationId) {
        return checkpointRepository.findByReservation_ReservationIdOrderByIssuedAtDesc(reservationId)
                .stream()
                .map(cp -> toDto(cp, false))
                .collect(Collectors.toList());
    }

    private CheckpointDTO toDto(ReservationCheckpoint checkpoint, boolean includePayload) {
        return CheckpointDTO.builder()
                .checkpointId(checkpoint.getCheckpointId())
                .reservationId(checkpoint.getReservation().getReservationId())
                .checkpointType(checkpoint.getCheckpointType())
                .status(checkpoint.getStatus())
                .qrToken(checkpoint.getQrToken())
                .qrPayload(includePayload ? buildQrPayload(checkpoint) : null)
                .issuedBy(checkpoint.getIssuedBy())
                .issuedAt(checkpoint.getIssuedAt())
                .expiresAt(checkpoint.getExpiresAt())
                .scannedAt(checkpoint.getScannedAt())
                .signedAt(checkpoint.getSignedAt())
                .signerName(checkpoint.getSignerName())
                .signerIdNumber(checkpoint.getSignerIdNumber())
                .signatureData(checkpoint.getSignatureData())
                .latitude(checkpoint.getLatitude())
                .longitude(checkpoint.getLongitude())
                .notes(checkpoint.getNotes())
                .build();
    }

    private String buildQrPayload(ReservationCheckpoint checkpoint) {
        String json = String.format(
                "{\"checkpointId\":%d,\"token\":\"%s\",\"type\":\"%s\",\"reservationId\":%d}",
                checkpoint.getCheckpointId(),
                checkpoint.getQrToken(),
                checkpoint.getCheckpointType(),
                checkpoint.getReservation().getReservationId()
        );
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unused")
    private String hashSignature(String signatureData, String seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update((signatureData + seed).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot hash signature", e);
        }
    }

    private boolean isWithinReservationWindow(Reservation reservation) {
        LocalDateTime now = LocalDateTime.now();
        if (reservation.getStartDatetime() == null || reservation.getEndDatetime() == null) {
            return false;
        }

        return !(now.isBefore(reservation.getStartDatetime()) || now.isAfter(reservation.getEndDatetime()));
    }

    private void updateReservationUsageStatus(Reservation reservation, CheckpointType type) {
        Reservation.Status newStatus = null;
        if (type == CheckpointType.CHECK_IN) {
            newStatus = Reservation.Status.IN_USE;
        } else if (type == CheckpointType.CHECK_OUT) {
            newStatus = Reservation.Status.COMPLETED;
        }

        if (newStatus != null && reservation.getStatus() != newStatus) {
            reservation.setStatus(newStatus);
            Reservation saved = reservationRepository.save(reservation);
            syncReservationStatusToAdmin(saved);
        }
    }

    private void syncReservationStatusToAdmin(Reservation reservation) {
        try {
            if (reservation.getReservationId() == null) {
                return;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("reservationId", reservation.getReservationId());
            payload.put("vehicleId", reservation.getVehicleId());
            payload.put("userId", reservation.getUserId());
            payload.put("startDatetime", reservation.getStartDatetime() != null ? reservation.getStartDatetime().toString() : null);
            payload.put("endDatetime", reservation.getEndDatetime() != null ? reservation.getEndDatetime().toString() : null);
            payload.put("purpose", reservation.getPurpose());
            payload.put("status", reservation.getStatus() != null ? reservation.getStatus().name() : Reservation.Status.BOOKED.name());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Sync-Origin", "reservation-service");
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            String url = adminServiceUrl + "/api/admin/reservations/" + reservation.getReservationId();
            restTemplate.exchange(url, HttpMethod.PUT, request, Void.class);
        } catch (Exception e) {
            System.err.println("⚠️ Không thể đồng bộ trạng thái sang admin service: " + e.getMessage());
        }
    }
}
