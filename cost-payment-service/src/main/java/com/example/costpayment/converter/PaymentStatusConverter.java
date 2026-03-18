package com.example.costpayment.converter;

import com.example.costpayment.entity.PaymentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, String> {

    @Override
    public String convertToDatabaseColumn(PaymentStatus attribute) {
        if (attribute == null) {
            System.err.println("PaymentStatusConverter: Received null status, returning null");
            return null;
        }
        
        // Convert enum to database format: use uppercase to match ENUM values in database
        // Database ENUM: 'PENDING','PAID','OVERDUE','CANCELLED'
        String dbValue;
        switch (attribute) {
            case PENDING:
                dbValue = "PENDING";
                break;
            case PAID:
                dbValue = "PAID";
                break;
            case OVERDUE:
                dbValue = "OVERDUE";
                break;
            case CANCELLED:
                dbValue = "CANCELLED";
                break;
            default:
                dbValue = attribute.name(); // Already uppercase
                System.err.println("PaymentStatusConverter: Unknown enum value: " + attribute + ", using: " + dbValue);
                break;
        }
        
        System.out.println("PaymentStatusConverter: Converting " + attribute + " -> " + dbValue);
        return dbValue;
    }

    @Override
    public PaymentStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return PaymentStatus.PENDING; // Default
        }
        
        // Normalize the database value to handle various formats
        String normalized = dbData.trim();
        
        // Handle common database formats and variations
        if (normalized.equalsIgnoreCase("Pending") || normalized.equalsIgnoreCase("PENDING")) {
            return PaymentStatus.PENDING;
        } else if (normalized.equalsIgnoreCase("Paid") || normalized.equalsIgnoreCase("PAID") ||
                   normalized.equalsIgnoreCase("Completed") || normalized.equalsIgnoreCase("COMPLETED")) {
            return PaymentStatus.PAID;
        } else if (normalized.equalsIgnoreCase("Overdue") || normalized.equalsIgnoreCase("OVERDUE")) {
            return PaymentStatus.OVERDUE;
        } else if (normalized.equalsIgnoreCase("Cancelled") || normalized.equalsIgnoreCase("CANCELLED") ||
                   normalized.equalsIgnoreCase("Canceled") || normalized.equalsIgnoreCase("CANCELED")) {
            return PaymentStatus.CANCELLED;
        }
        
        // Try direct enum value match
        try {
            return PaymentStatus.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Default fallback
            return PaymentStatus.PENDING;
        }
    }
}

