# Applying the Hierarchy Guard documentation pack

## Target

Repository:

```text
cheshmapoosh/digiaudit-grcone
```

Branch reviewed:

```text
feature/master-data-v2-greenfield
```

## Package contents

### Complete replacement/new files

Copy these paths into the repository:

```text
AGENTS.md
grcone-app/AGENTS.md
grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/AGENTS.md
grcone-app/src/main/java/com/digiaudit/grcone/modules/organization/AGENTS.md
grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/process/AGENTS.md
grcone-docs/master-data/hierarchy-guard-row-contract.md
grcone-docs/architecture/decisions/ADR-0001-database-hierarchy-guard-row.md
grcone-docs/architecture/decisions/ADR-0001-database-hierarchy-guard-row_FA.md
```

### Alignment patch for large existing planning documents

```text
grcone-docs/master-data/master-data-hierarchy-guard-alignment.patch
```

From the repository root:

```bash
git apply --check grcone-docs/master-data/master-data-hierarchy-guard-alignment.patch
git apply grcone-docs/master-data/master-data-hierarchy-guard-alignment.patch
```

Run the check before applying because the branch may have moved since 2026-08-04.

## What is intentionally not changed

The retained authoritative Word files are not rewritten in this pack:

```text
grcone-docs/master-data/GRC_Master_Data_Reference_Conceptual_Model_FA.docx
grcone-docs/master-data/GRC_Master_Data_Logical_Model_Final_FA.docx
grcone-docs/master-data/GRC_Master_Data_Physical_Design_Reference_FA.docx
```

The accepted owner correction is recorded as an ADR and binding implementation contract. The authority hierarchy states that the correction supersedes older wording only for hierarchy concurrency and the technical-table count.

## Corrected count

```text
45 business tables
2 technical tables
47 physical tables total
```

Technical tables:

```text
document_temp_upload
masterdata_hierarchy_guard
```
