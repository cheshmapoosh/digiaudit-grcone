import { useEffect, useRef, useState, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { useTranslation } from "react-i18next";
import { BusyIndicator, Button, Dialog, FlexBox, MessageStrip, ProgressIndicator, Text } from "@ui5/webcomponents-react";
import { useDialogOpenAfterRender } from "@/shared/hooks/useDialogOpenAfterRender";
import { CatalogActionButton } from "./CatalogActionButton";
import { DialogFooterBar } from "./DialogFooter";
import { ModalDialogHeader } from "./ModalDialogHeader";
import { SingleFileUploader } from "./SingleFileUploader";
import { showSuccessToast } from "@/shared/feedback/toast.store";
import "./workbook-import-dialog.css";

type Progress = (percent: number) => void;
type Operation = "preview" | "download" | "commit";
type ValidatedWorkbook = { valid: boolean; fingerprint: string | null };

interface WorkbookImportDialogProps<T extends ValidatedWorkbook> {
  title: string;
  templateName: string;
  downloadTemplate: () => Promise<Blob>;
  previewWorkbook: (file: File, signal: AbortSignal, progress: Progress) => Promise<T>;
  commitWorkbook: (file: File, fingerprint: string, progress: Progress) => Promise<{ count: number }>;
  refreshedPreview: (failure: unknown) => T | null;
  renderPreview: (preview: T) => ReactNode;
  onClose: () => void;
  onImported: () => Promise<unknown>;
}

export function WorkbookImportDialog<T extends ValidatedWorkbook>({
  title, templateName, downloadTemplate, previewWorkbook, commitWorkbook, refreshedPreview,
  renderPreview, onClose, onImported,
}: WorkbookImportDialogProps<T>) {
  const { t, i18n } = useTranslation();
  const open = useDialogOpenAfterRender();
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [operation, setOperation] = useState<Operation | null>(null);
  const [progress, setProgress] = useState(0);
  const running = useRef(false);
  const previewRequest = useRef<AbortController | null>(null);
  const busy = operation !== null;
  const transferring = operation === "preview" || operation === "commit";
  const uploadLabel = t(operation === "commit" ? "catalogImport.sendingForSave" : "catalogImport.sending");
  const processingLabel = t(operation === "commit" ? "catalogImport.saving" : "catalogImport.validating");
  useEffect(() => () => previewRequest.current?.abort(), []);

  const run = async (nextOperation: Operation, action: () => Promise<void>) => {
    if (running.current) return;
    running.current = true;
    setOperation(nextOperation);
    setProgress(0);
    setError(null);
    try {
      await action();
    } catch (cause) {
      if (!(cause instanceof Error && cause.name === "AbortError"))
        setError(cause instanceof Error ? cause.message : t("catalogImport.failed"));
    } finally {
      running.current = false;
      setOperation(null);
    }
  };

  const selectFile = (chosen: File) => {
    if (running.current) return;
    setFile(chosen);
    setPreview(null);
    void run("preview", async () => {
      if (!chosen.name.toLowerCase().endsWith(".xlsx")) {
        setError(t("catalogImport.issues.FILE_FORMAT"));
        return;
      }
      if (!chosen.size || chosen.size > 2 * 1024 * 1024) {
        setError(t("catalogImport.issues.FILE_SIZE"));
        return;
      }
      const controller = new AbortController();
      previewRequest.current = controller;
      const result = await previewWorkbook(chosen, controller.signal, setProgress);
      if (!controller.signal.aborted) setPreview(result);
    });
  };

  const download = () => void run("download", async () => {
    const url = URL.createObjectURL(await downloadTemplate());
    const link = document.createElement("a");
    link.href = url;
    link.download = templateName;
    link.click();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  });

  const commit = () => {
    if (!file || !preview?.valid || !preview.fingerprint) return;
    const fingerprint = preview.fingerprint;
    void run("commit", async () => {
      let count: number;
      try {
        ({ count } = await commitWorkbook(file, fingerprint, setProgress));
      } catch (cause) {
        const updated = refreshedPreview(cause);
        if (!updated) throw cause;
        setPreview(updated);
        setError(t("catalogImport.changed"));
        return;
      }
      setPreview(null);
      setFile(null);
      showSuccessToast(t("catalogImport.saved", { total: new Intl.NumberFormat(i18n.language).format(count) }));
      try {
        await onImported();
        onClose();
      } catch {
        setError(t("catalogImport.refreshFailed"));
      }
    });
  };

  const close = () => { if (!running.current) onClose(); };
  return createPortal(<Dialog open={open} className="catalogObjectDialog workbookImportDialog" accessibleName={title}
    onClose={close} onBeforeClose={(event) => { if (running.current) event.preventDefault(); }}
    footer={<DialogFooterBar>
      <CatalogActionButton design="Emphasized" disabled={busy || !preview?.valid || !preview.fingerprint} onClick={commit}>
        {t("common.save")}
      </CatalogActionButton>
      <CatalogActionButton disabled={busy} onClick={close}>{t("common.close")}</CatalogActionButton>
    </DialogFooterBar>}>
    <ModalDialogHeader title={title} onClose={close} />
    <FlexBox direction="Column" className="workbookImportContent" data-catalog-dialog-content dir={i18n.dir()}>
      <FlexBox wrap="Wrap" alignItems="Center" className="workbookImportActions">
        <Button icon="download" disabled={busy} onClick={download}>{t("catalogImport.template")}</Button>
        <SingleFileUploader accept=".xlsx" disabled={busy} accessibleName={t("catalogImport.choose")} onSelect={selectFile}>
          <Button disabled={busy}>{t("catalogImport.choose")}</Button>
        </SingleFileUploader>
        {file ? <Text className="workbookImportFileName" maxLines={1} title={file.name}>{file.name}</Text> : null}
      </FlexBox>
      {transferring ? <FlexBox direction="Column" className="workbookImportActions">
        <Text>{uploadLabel}</Text>
        <ProgressIndicator value={progress} valueState="Information" accessibleName={uploadLabel}
          displayValue={new Intl.NumberFormat(i18n.language, { style: "percent" }).format(progress / 100)} />
        {progress === 100 ? <FlexBox alignItems="Center" className="workbookImportActions" role="status">
          <BusyIndicator active delay={0} size="S" />
          <Text>{processingLabel}</Text>
        </FlexBox> : null}
      </FlexBox> : null}
      {operation === "download" ? <BusyIndicator active delay={0} aria-label={t("catalogImport.downloading")} /> : null}
      {error ? <MessageStrip design="Negative" hideCloseButton>{error}</MessageStrip> : null}
      {preview ? renderPreview(preview) : null}
    </FlexBox>
  </Dialog>, document.body);
}
