package ru.pifms.server.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RoleTypeConverter implements AttributeConverter<Role.RoleType, String> {

    @Override
    public String convertToDatabaseColumn(Role.RoleType attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public Role.RoleType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Role.RoleType.fromDbValue(dbData);
    }
}