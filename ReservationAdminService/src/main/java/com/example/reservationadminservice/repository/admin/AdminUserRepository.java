package com.example.reservationadminservice.repository.admin;

import com.example.reservationadminservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
