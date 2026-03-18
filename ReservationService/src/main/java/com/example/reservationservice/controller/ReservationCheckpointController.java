package com.example.reservationservice.controller;

import com.example.reservationservice.dto.CheckpointDTO;
import com.example.reservationservice.dto.CheckpointIssueRequest;
import com.example.reservationservice.dto.CheckpointScanRequest;
import com.example.reservationservice.dto.CheckpointSignRequest;
import com.example.reservationservice.service.ReservationCheckpointService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = {"http://localhost:8080"}, allowCredentials = "true")
@RequiredArgsConstructor
public class ReservationCheckpointController {

    private final ReservationCheckpointService checkpointService;

    @PostMapping("/{reservationId}/checkpoints")
    public CheckpointDTO issue(@PathVariable Long reservationId,
                               @RequestBody CheckpointIssueRequest request) {
        return checkpointService.issueCheckpoint(reservationId, request);
    }

    @GetMapping("/{reservationId}/checkpoints")
    public List<CheckpointDTO> list(@PathVariable Long reservationId) {
        return checkpointService.getCheckpointsForReservation(reservationId);
    }

    @PostMapping("/checkpoints/scan")
    public CheckpointDTO scan(@RequestBody CheckpointScanRequest request) {
        return checkpointService.scanCheckpoint(request);
    }

    @PostMapping("/checkpoints/{checkpointId}/sign")
    public CheckpointDTO sign(@PathVariable Long checkpointId,
                              @RequestBody CheckpointSignRequest request) {
        return checkpointService.signCheckpoint(checkpointId, request);
    }
}

