package com.digiaudit.grcone.modules.masterdata.revision.application;

@FunctionalInterface
public interface RevisionOperation {
    RevisionOperationResult execute(RevisionExecutionContext context);
}
