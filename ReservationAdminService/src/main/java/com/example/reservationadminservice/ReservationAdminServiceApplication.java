package com.example.reservationadminservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReservationAdminServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReservationAdminServiceApplication.class, args);
    }
}
