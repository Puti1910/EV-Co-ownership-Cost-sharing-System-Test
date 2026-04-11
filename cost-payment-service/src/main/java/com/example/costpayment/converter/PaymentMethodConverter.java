package com.example.costpayment.converter;

import com.example.costpayment.entity.Payment;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PaymentMethodConverter implements AttributeConverter<Payment.Method, String> {

    @Override
    public String convertToDatabaseColumn(Payment.Method attribute) {
        if (attribute == null) {
            return null;
        }
        // Convert enum to database format: use uppercase to match ENUM values in database
        // Database ENUM: 'EWALLET','BANKING','CASH'
        switch (attribute) {
            case EWALLET:
                return "EWALLET";
            case BANKING:
                return "BANKING";
            case CASH:
                return "CASH";
            default:
                return attribute.name(); // Already uppercase
        }
    }

    @Override
    public Payment.Method convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return Payment.Method.EWALLET; // Default
        }
        
        // Normalize the database value to handle various formats
        String normalized = dbData.trim();
        
        // Handle database format (EWallet, Banking, Cash)
        if (normalized.equalsIgnoreCase("EWallet") || normalized.equalsIgnoreCase("EWALLET")) {
            return Payment.Method.EWALLET;
        } else if (normalized.equalsIgnoreCase("Banking") || normalized.equalsIgnoreCase("BANKING") || 
                   normalized.equalsIgnoreCase("BankTransfer") || normalized.equalsIgnoreCase("BANKTRANSFER")) {
            return Payment.Method.BANKING;
        } else if (normalized.equalsIgnoreCase("Cash") || normalized.equalsIgnoreCase("CASH")) {
            return Payment.Method.CASH;
        }
        
        // Try direct enum value match
        try {
            return Payment.Method.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Default fallback
            return Payment.Method.EWALLET;
        }
    }
}

