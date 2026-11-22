package com.digiaudit.grcone.modules.securityacl.application;

import static com.digiaudit.grcone.common.util.Texts.normalizeRequired;

import com.digiaudit.grcone.common.exception.ConflictException;
import com.digiaudit.grcone.common.exception.NotFoundException;
import com.digiaudit.grcone.common.security.CurrentUserProvider;
import com.digiaudit.grcone.modules.audit.application.AuditService;
import com.digiaudit.grcone.modules.audit.domain.enums.ActionResult;
import com.digiaudit.grcone.modules.audit.domain.enums.AuditEventType;
import com.digiaudit.grcone.modules.audit.domain.enums.AuditTargetType;
import com.digiaudit.grcone.modules.securityacl.api.dto.ResourceAclEntryRequest;
import com.digiaudit.grcone.modules.securityacl.api.dto.ResourceAclEntryResponse;
import com.digiaudit.grcone.modules.securityacl.api.mapper.ResourceAclEntryMapper;
import com.digiaudit.grcone.modules.securityacl.domain.entity.ResourceAclEntryEntity;
import com.digiaudit.grcone.modules.securityacl.domain.repository.ResourceAclEntryRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceAclEntryService {

    private final ResourceAclEntryRepository repository;
    private final ResourceAclEntryMapper mapper;
    private final AuditService auditService;
    private final CurrentUserProvider currentUserProvider;
    private final ProfileAclBoundary profileBoundary;

    public List<ResourceAclEntryResponse> list(String targetType, UUID targetId) {
        profileBoundary.target(targetType, targetId);
        return repository.findByTargetTypeAndTargetId(targetType, targetId).stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public ResourceAclEntryResponse upsert(ResourceAclEntryRequest request, HttpServletRequest httpRequest) {
        profileBoundary.target(request.targetType(), request.targetId());
        profileBoundary.subject(request.subjectType(), request.subjectId());
        if (request.validFrom() != null && request.validTo() != null && request.validTo().isBefore(request.validFrom())) {
            throw new ConflictException("ACL_INVALID_RANGE", "error.masterdata.invalidDateRange", "ACL validTo cannot be before validFrom");
        }
        ResourceAclEntryEntity entity = repository.findByTargetTypeAndTargetIdAndSubjectTypeAndSubjectIdAndPermissionCode(
                        normalizeRequired(request.targetType()),
                        request.targetId(),
                        normalizeRequired(request.subjectType()),
                        request.subjectId(),
                        normalizeRequired(request.permissionCode()))
                .orElseGet(ResourceAclEntryEntity::new);
        entity.setTargetType(normalizeRequired(request.targetType()));
        entity.setTargetId(request.targetId());
        entity.setSubjectType(normalizeRequired(request.subjectType()));
        entity.setSubjectId(request.subjectId());
        entity.setPermissionCode(normalizeRequired(request.permissionCode()));
        entity.setEffect(normalizeRequired(request.effect()));
        entity.setValidFrom(request.validFrom());
        entity.setValidTo(request.validTo());
        ResourceAclEntryEntity saved = repository.save(entity);
        audit("ACL_UPSERTED", saved.getId(), httpRequest, Map.of("targetType", saved.getTargetType(), "targetId", saved.getTargetId()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id, HttpServletRequest httpRequest) {
        ResourceAclEntryEntity entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("ACL_NOT_FOUND", "error.masterdata.notFound", "ACL entry not found: " + id, id));
        repository.delete(entity);
        audit("ACL_DELETED", id, httpRequest, Map.of("targetType", entity.getTargetType(), "targetId", entity.getTargetId()));
    }

    private void audit(String eventName, UUID targetId, HttpServletRequest request, Map<String, Object> details) {
        Map<String, Object> safeDetails = new LinkedHashMap<>();
        safeDetails.put("event", eventName);
        safeDetails.putAll(details);
        auditService.log(AuditEventType.ACL_CHANGED, AuditTargetType.RESOURCE_ACL, targetId.toString(), ActionResult.SUCCESS, currentUserProvider.getCurrentUserIdOrNull(), request, safeDetails);
    }
}
