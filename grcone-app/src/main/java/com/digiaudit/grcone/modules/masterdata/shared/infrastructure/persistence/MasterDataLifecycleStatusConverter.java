package com.digiaudit.grcone.modules.masterdata.shared.infrastructure.persistence;

import com.digiaudit.grcone.modules.masterdata.shared.domain.MasterDataLifecycleStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class MasterDataLifecycleStatusConverter implements AttributeConverter<MasterDataLifecycleStatus, String> {
    @Override
    public String convertToDatabaseColumn(MasterDataLifecycleStatus attribute) {
        return attribute == null ? null : attribute.wireValue();
    }

    @Override
    public MasterDataLifecycleStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MasterDataLifecycleStatus.fromWireValue(dbData);
    }
}
