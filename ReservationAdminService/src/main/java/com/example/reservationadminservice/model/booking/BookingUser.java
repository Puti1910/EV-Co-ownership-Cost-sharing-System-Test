package com.example.reservationadminservice.model.booking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity cho bảng users trong booking database (read-only)
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class BookingUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "full_name")
    private String fullName;
    
    @Column(name = "username")
    private String username;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "phone")
    private String phone;
    
    @Column(name = "password")
    private String password;
}


