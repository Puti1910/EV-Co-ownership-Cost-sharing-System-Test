package com.example.groupmanagement.entity.converter;

import com.example.groupmanagement.entity.GroupContract;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ContractStatusConverter implements AttributeConverter<GroupContract.ContractStatus, String> {

    @Override
    public String convertToDatabaseColumn(GroupContract.ContractStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name().toLowerCase();
    }

    @Override
    public GroupContract.ContractStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return GroupContract.ContractStatus.PENDING;
        }
        try {
            return GroupContract.ContractStatus.valueOf(dbData.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return GroupContract.ContractStatus.PENDING;
        }
    }
}


