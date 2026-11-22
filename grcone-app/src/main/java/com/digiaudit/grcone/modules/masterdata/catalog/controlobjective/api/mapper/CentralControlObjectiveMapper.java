package com.digiaudit.grcone.modules.masterdata.catalog.controlobjective.api.mapper;

import com.digiaudit.grcone.modules.masterdata.catalog.controlobjective.api.dto.CentralControlObjectiveResponse;
import com.digiaudit.grcone.modules.masterdata.catalog.controlobjective.api.dto.CentralControlObjectiveSummaryResponse;
import com.digiaudit.grcone.modules.masterdata.catalog.controlobjective.domain.entity.CentralControlObjectiveEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CentralControlObjectiveMapper {
  CentralControlObjectiveResponse toResponse(CentralControlObjectiveEntity entity);

  CentralControlObjectiveSummaryResponse toSummary(CentralControlObjectiveEntity entity);
}
