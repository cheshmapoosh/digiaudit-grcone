package com.digiaudit.grcone.modules.organization.infrastructure.persistence;

import com.digiaudit.grcone.modules.organization.domain.OrganizationType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class OrganizationTypeConverter implements AttributeConverter<OrganizationType, String> {
    @Override
    public String convertToDatabaseColumn(OrganizationType attribute) {
        return attribute == null ? null : attribute.wireValue();
    }

    @Override
    public OrganizationType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OrganizationType.fromWireValue(dbData);
    }
}
