package com.example.reservationadminservice.service;

import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Service
public class BookingUserService {
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/co_ownership_booking?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    
    public BookingUserService() {
        System.out.println("✓ BookingUserService initialized - will connect directly to booking database");
    }
    
    public String getUserFullName(Long userId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            // Tạo connection trực tiếp đến booking database
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
            String sql = "SELECT full_name FROM users WHERE user_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, userId);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                String fullName = rs.getString("full_name");
                System.out.println("✓ Found user: userId=" + userId + ", fullName=" + fullName);
                return fullName;
            } else {
                System.err.println("✗ No user found with userId=" + userId);
                return null;
            }
        } catch (Exception e) {
            System.err.println("✗ Error finding user with userId=" + userId + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
}

