package com.example.reservationservice.repository;

import com.example.reservationservice.model.ReservationCheckpoint;
import com.example.reservationservice.model.ReservationCheckpoint.CheckpointStatus;
import com.example.reservationservice.model.ReservationCheckpoint.CheckpointType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationCheckpointRepository extends JpaRepository<ReservationCheckpoint, Long> {

    List<ReservationCheckpoint> findByReservation_ReservationIdOrderByIssuedAtDesc(Long reservationId);

    Optional<ReservationCheckpoint> findByQrToken(String qrToken);

    @Query("""
            SELECT c FROM ReservationCheckpoint c
            WHERE c.reservation.reservationId = :reservationId
              AND c.checkpointType = :checkpointType
              AND c.status IN (:statuses)
            """)
    List<ReservationCheckpoint> findActiveByReservationAndType(@Param("reservationId") Long reservationId,
                                                               @Param("checkpointType") CheckpointType checkpointType,
                                                               @Param("statuses") List<CheckpointStatus> statuses);

    @Query("""
            SELECT c FROM ReservationCheckpoint c
            WHERE c.expiresAt IS NOT NULL
              AND c.expiresAt < :threshold
              AND c.status IN ('PENDING','SCANNED','SIGNED')
            """)
    List<ReservationCheckpoint> findExpired(@Param("threshold") LocalDateTime threshold);
}

