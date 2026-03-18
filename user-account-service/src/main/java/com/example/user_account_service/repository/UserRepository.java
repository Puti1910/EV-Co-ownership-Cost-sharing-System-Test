package com.example.user_account_service.repository;

import com.example.user_account_service.entity.User;
import com.example.user_account_service.enums.ProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByIdCardNumber(String idCardNumber);

    List<User> findByProfileStatus(ProfileStatus status);
}