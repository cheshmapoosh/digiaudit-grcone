package com.digiaudit.grcone.modules.document.infrastructure.persistence;

import com.digiaudit.grcone.modules.document.domain.DocumentLinkTargetType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DocumentLinkTargetTypeConverter implements AttributeConverter<DocumentLinkTargetType, String> {
    @Override
    public String convertToDatabaseColumn(DocumentLinkTargetType attribute) {
        return attribute == null ? null : attribute.wireValue();
    }

    @Override
    public DocumentLinkTargetType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DocumentLinkTargetType.fromWireValue(dbData);
    }
}
