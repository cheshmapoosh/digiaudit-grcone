package com.digiaudit.grcone.modules.masterdata.revision.infrastructure.persistence;

import com.digiaudit.grcone.modules.masterdata.revision.domain.RevisionEntityType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RevisionEntityTypeConverter implements AttributeConverter<RevisionEntityType, String> {
    @Override
    public String convertToDatabaseColumn(RevisionEntityType attribute) {
        return attribute == null ? null : attribute.wireValue();
    }

    @Override
    public RevisionEntityType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RevisionEntityType.fromWireValue(dbData);
    }
}
