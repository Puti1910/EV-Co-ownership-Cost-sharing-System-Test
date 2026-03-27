package com.example.reservationservice.repository;

import com.example.reservationservice.model.Reservation;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
        SELECT COUNT(r) FROM Reservation r
        WHERE r.vehicleId = :vehicleId
          AND r.status IN ('BOOKED','IN_USE')
          AND r.startDatetime < :end
          AND r.endDatetime > :start
    """)
    long countOverlap(@Param("vehicleId") Integer vehicleId,
                      @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end);

    List<Reservation> findByVehicleIdOrderByStartDatetimeAsc(Integer vehicleId);

    List<Reservation> findByUserIdAndVehicleIdOrderByStartDatetimeAsc(Integer userId, Integer vehicleId);

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.vehicleId = :vehicleId
          AND r.startDatetime >= :rangeStart
          AND r.endDatetime <= :rangeEnd
        ORDER BY r.startDatetime ASC
    """)
    List<Reservation> findByVehicleAndRange(@Param("vehicleId") Integer vehicleId,
                                            @Param("rangeStart") LocalDateTime rangeStart,
                                            @Param("rangeEnd") LocalDateTime rangeEnd);
}
