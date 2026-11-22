import { useTranslation } from "react-i18next";
import { WorkbookImportDialog } from "@/shared/components/WorkbookImportDialog";
import { HttpError } from "@/shared/infra/http.client";
import { CatalogImportPreviewTabs } from "./CatalogImportPreviewTabs";
import { catalogImportApi, type CatalogImportFamily, type CatalogImportPreview } from "./catalogImport.api";

function refreshedPreview(failure: unknown, family: CatalogImportFamily) {
  if (!(failure instanceof HttpError)
    || (failure.code !== "CATALOG_IMPORT_STALE" && failure.code !== "CATALOG_IMPORT_INVALID")) return null;
  const preview = (failure.data as { preview?: CatalogImportPreview } | null)?.preview;
  return preview?.family === family ? preview : null;
}

export function CatalogImportDialog({ family, onClose, onImported }: {
  family: CatalogImportFamily;
  onClose: () => void;
  onImported: () => Promise<unknown>;
}) {
  const { t } = useTranslation();
  return <WorkbookImportDialog
    title={t("catalogImport.title", { family: t("catalogImport.families." + family) })}
    templateName={family.toLowerCase().replaceAll("_", "-") + "-template.xlsx"}
    downloadTemplate={() => catalogImportApi.template(family)}
    previewWorkbook={(file, signal, progress) => catalogImportApi.preview(family, file, signal, progress)}
    commitWorkbook={(file, fingerprint, progress) => catalogImportApi.commit(family, file, fingerprint, progress)}
    refreshedPreview={(failure) => refreshedPreview(failure, family)}
    renderPreview={(preview) => <CatalogImportPreviewTabs preview={preview} />}
    onClose={onClose}
    onImported={onImported}
  />;
}
