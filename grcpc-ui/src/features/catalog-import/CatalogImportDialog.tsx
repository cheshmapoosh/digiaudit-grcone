import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { useTranslation } from "react-i18next";
import {
  BusyIndicator, Button, Dialog, FlexBox, MessageStrip, ObjectStatus, ProgressIndicator, Table, TableCell, TableHeaderCell,
  TableHeaderRow, TableRow, Text, Title, Tree, TreeItem, TreeItemCustom,
} from "@ui5/webcomponents-react";
import { CatalogActionButton } from "@/shared/components/CatalogActionButton";
import { CatalogTableViewport } from "@/shared/components/CatalogTableViewport";
import { DialogFooterBar } from "@/shared/components/DialogFooter";
import { ModalDialogHeader } from "@/shared/components/ModalDialogHeader";
import { SingleFileUploader } from "@/shared/components/SingleFileUploader";
import { showSuccessToast } from "@/shared/feedback/toast.store";
import { useDialogOpenAfterRender } from "@/shared/hooks/useDialogOpenAfterRender";
import { HttpError } from "@/shared/infra/http.client";
import { formatPersianDate } from "@/shared/utils/date.utils";
import { catalogImportApi, type CatalogImportFamily, type CatalogImportPreview, type CatalogImportRow } from "./catalogImport.api";
import "./catalog-import.css";

interface TreeNode {
  key: string;
  row?: CatalogImportRow;
  existingParent?: { code: string; title: string; kind?: string | null };
  children: TreeNode[];
}

const ISSUE_PAGE_SIZE = 40;

function makeForest(rows: CatalogImportRow[]) {
  const nodes = new Map(rows.map((row) => [row.rowNumber, {
    key: String(row.rowNumber), row, children: [],
  } satisfies TreeNode] as [number, TreeNode]));
  const roots: TreeNode[] = [];
  const externalParents = new Map<string, TreeNode>();
  const unresolved: TreeNode[] = [];
  for (const row of rows) {
    const node = nodes.get(row.rowNumber)!;
    if (row.parentRow !== null) {
      const visited = new Set([row.rowNumber]);
      let ancestor: number | null | undefined = row.parentRow;
      while (ancestor != null && !visited.has(ancestor)) {
        visited.add(ancestor);
        ancestor = nodes.get(ancestor)?.row?.parentRow;
      }
      const parent = nodes.get(row.parentRow);
      if (ancestor == null && parent) parent.children.push(node);
      else unresolved.push(node);
    } else if (row.existingParentTitle !== null) {
      const parentKey = `${row.existingParentKind ?? ""}:${row.parentCode}`;
      let external = externalParents.get(parentKey);
      if (!external) {
        external = {
          key: "external-" + parentKey,
          existingParent: { code: row.parentCode, title: row.existingParentTitle, kind: row.existingParentKind },
          children: [],
        };
        externalParents.set(parentKey, external);
        roots.push(external);
      }
      external.children.push(node);
    } else if (row.parentCode) unresolved.push(node);
    else roots.push(node);
  }
  return { roots, unresolved };
}

function ImportTreeItem({ node }: { node: TreeNode }) {
  const { t, i18n } = useTranslation();
  const row = node.row;
  const title = row?.title ?? node.existingParent?.title;
  const code = row?.code ?? node.existingParent?.code;
  const kind = row?.kind ?? node.existingParent?.kind;
  return <TreeItemCustom expanded={node.children.length > 0} content={<div className="catalogImportTreeRow">
    {title ? <bdi className="catalogImportTreeTitle">{title}</bdi> : null}
    {code ? <bdi dir="ltr" className="catalogImportTreeCode">{code}</bdi> : null}
    <span className="catalogImportTreeDetails">
      <span>{row
        ? t("catalogImport.row", { row: new Intl.NumberFormat(i18n.language).format(row.rowNumber) })
        : t("catalogImport.existingParent")}</span>
      {kind ? <bdi>{t("catalogImport.kinds." + kind, { defaultValue: kind })}</bdi> : null}
      {row && !row.valid ? <ObjectStatus state="Negative">{t("catalogImport.rowInvalid")}</ObjectStatus> : null}
    </span>
  </div>}>
    {node.children.map((child) => <ImportTreeItem key={child.key} node={child} />)}
  </TreeItemCustom>;
}

function ImportPreview({ preview }: { preview: CatalogImportPreview }) {
  const { t, i18n } = useTranslation();
  const [issuePage, setIssuePage] = useState(0);
  const issuePages = Math.ceil(preview.issues.length / ISSUE_PAGE_SIZE);
  const currentPage = Math.min(issuePage, Math.max(0, issuePages - 1));
  const firstIssue = currentPage * ISSUE_PAGE_SIZE;
  const number = (value: number) => new Intl.NumberFormat(i18n.language).format(value);
  const { roots, unresolved } = makeForest(preview.rows);
  const flat = preview.family === "CONTROL_OBJECTIVE";
  const flatColumns = ["ROW", "CODE", "TITLE", "VALID_FROM", "VALID_TO"] as const;
  return <>
    <ObjectStatus state={preview.valid ? "Positive" : "Negative"}>
      {preview.valid
        ? t("catalogImport.ready", { total: number(preview.rows.length) })
        : t("catalogImport.invalid", { total: number(preview.issues.length) })}
    </ObjectStatus>
    {preview.rows.length > 0 ? <section aria-label={t("catalogImport.preview")}>
      <Title level="H5">{t("catalogImport.preview")}</Title>
      {flat ? <CatalogTableViewport accessibleName={t("catalogImport.preview")}>
        <Table overflowMode="Scroll" headerRow={<TableHeaderRow>{flatColumns.map((column) =>
          <TableHeaderCell key={column}>{t("catalogImport.fields." + column)}</TableHeaderCell>)}</TableHeaderRow>}>
          {preview.rows.map((row) => <TableRow key={row.rowNumber}>
            <TableCell>{number(row.rowNumber)}</TableCell><TableCell>{row.code}</TableCell><TableCell>{row.title}</TableCell>
            <TableCell>{row.validFrom ? formatPersianDate(row.validFrom) : "—"}</TableCell>
            <TableCell>{row.validTo ? formatPersianDate(row.validTo) : "—"}</TableCell>
          </TableRow>)}
        </Table>
      </CatalogTableViewport> : <Tree className="catalogImportTree" accessibleName={t("catalogImport.preview")}>
        {roots.map((node) => <ImportTreeItem key={node.key} node={node} />)}
        {unresolved.length ? <TreeItem text={t("catalogImport.unresolved")} expanded>
          {unresolved.map((node) => <ImportTreeItem key={node.key} node={node} />)}
        </TreeItem> : null}
      </Tree>}
    </section> : null}
    {preview.issues.length ? <section aria-label={t("catalogImport.errors")}>
      <Title level="H5">{t("catalogImport.errors")}</Title>
      <CatalogTableViewport accessibleName={t("catalogImport.errors")}>
        <Table overflowMode="Scroll" headerRow={<TableHeaderRow>
          <TableHeaderCell>{t("catalogImport.fields.ROW")}</TableHeaderCell>
          <TableHeaderCell>{t("catalogImport.column")}</TableHeaderCell>
          <TableHeaderCell>{t("catalogImport.problem")}</TableHeaderCell>
        </TableHeaderRow>}>
          {preview.issues.slice(firstIssue, firstIssue + ISSUE_PAGE_SIZE).map((issue, index) => <TableRow key={firstIssue + index}>
            <TableCell>{issue.rowNumber > 0 ? number(issue.rowNumber) : "—"}</TableCell>
            <TableCell>{t("catalogImport.fields." + issue.field, { defaultValue: issue.field })}</TableCell>
            <TableCell>{t("catalogImport.issues." + issue.code, { defaultValue: t("catalogImport.failed") })}</TableCell>
          </TableRow>)}
        </Table>
      </CatalogTableViewport>
      {issuePages > 1 ? <FlexBox alignItems="Center" justifyContent="Center" className="catalogImportActions">
        <Button disabled={currentPage === 0} onClick={() => setIssuePage(currentPage - 1)}>{t("catalogImport.previous")}</Button>
        <Text>{t("catalogImport.page", { page: number(currentPage + 1), pages: number(issuePages) })}</Text>
        <Button disabled={currentPage + 1 >= issuePages} onClick={() => setIssuePage(currentPage + 1)}>{t("catalogImport.next")}</Button>
      </FlexBox> : null}
    </section> : null}
  </>;
}

function failedPreview(error: HttpError, family: CatalogImportFamily) {
  if (error.code !== "CATALOG_IMPORT_STALE" && error.code !== "CATALOG_IMPORT_INVALID") return null;
  const preview = (error.data as { preview?: CatalogImportPreview } | null)?.preview;
  return preview?.family === family ? preview : null;
}

export function CatalogImportDialog({ family, onClose, onImported }: {
  family: CatalogImportFamily;
  onClose: () => void;
  onImported: () => Promise<unknown>;
}) {
  const { t, i18n } = useTranslation();
  const open = useDialogOpenAfterRender();
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<CatalogImportPreview | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [operation, setOperation] = useState<"preview" | "download" | "commit" | null>(null);
  const [progress, setProgress] = useState(0);
  const running = useRef(false);
  const previewRequest = useRef<AbortController | null>(null);
  const title = t("catalogImport.title", { family: t("catalogImport.families." + family) });
  const busy = operation !== null;
  const transferring = operation === "preview" || operation === "commit";
  const uploadLabel = t(operation === "commit" ? "catalogImport.sendingForSave" : "catalogImport.sending");
  const processingLabel = t(operation === "commit" ? "catalogImport.saving" : "catalogImport.validating");
  useEffect(() => () => previewRequest.current?.abort(), []);

  const run = async (nextOperation: NonNullable<typeof operation>, action: () => Promise<void>) => {
    if (running.current) return;
    running.current = true;
    setOperation(nextOperation);
    setProgress(0);
    setError(null);
    try { await action(); }
    catch (cause) {
      if (!(cause instanceof Error && cause.name === "AbortError"))
        setError(cause instanceof HttpError ? cause.message : t("catalogImport.failed"));
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
      const result = await catalogImportApi.preview(family, chosen, controller.signal, setProgress);
      if (!controller.signal.aborted) setPreview(result);
    });
  };

  const download = () => void run("download", async () => {
    const url = URL.createObjectURL(await catalogImportApi.template(family));
    const link = document.createElement("a");
    link.href = url;
    link.download = family.toLowerCase().replaceAll("_", "-") + "-template.xlsx";
    link.click();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  });

  const commit = () => {
    if (!file || !preview?.valid || !preview.fingerprint) return;
    const fingerprint = preview.fingerprint;
    void run("commit", async () => {
      let count: number;
      try { ({ count } = await catalogImportApi.commit(family, file, fingerprint, setProgress)); }
      catch (cause) {
        const updated = cause instanceof HttpError ? failedPreview(cause, family) : null;
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
  return createPortal(<Dialog open={open} className="catalogObjectDialog" accessibleName={title}
    onClose={close} onBeforeClose={(event) => { if (running.current) event.preventDefault(); }}
    footer={<DialogFooterBar>
      <CatalogActionButton design="Emphasized" disabled={busy || !preview?.valid || !preview.fingerprint} onClick={commit}>
        {t("common.save")}
      </CatalogActionButton>
      <CatalogActionButton disabled={busy} onClick={close}>{t("common.close")}</CatalogActionButton>
    </DialogFooterBar>}>
    <ModalDialogHeader title={title} onClose={close} />
    <FlexBox direction="Column" className="catalogImportContent" data-catalog-dialog-content dir={i18n.dir()}>
      <FlexBox wrap="Wrap" alignItems="Center" className="catalogImportActions">
        <Button icon="download" disabled={busy} onClick={download}>{t("catalogImport.template")}</Button>
        <SingleFileUploader accept=".xlsx" disabled={busy} accessibleName={t("catalogImport.choose")} onSelect={selectFile}>
          <Button disabled={busy}>{t("catalogImport.choose")}</Button>
        </SingleFileUploader>
        {file ? <Text>{file.name}</Text> : null}
      </FlexBox>
      {transferring ? <FlexBox direction="Column" className="catalogImportActions">
        <Text>{uploadLabel}</Text>
        <ProgressIndicator value={progress} valueState="Information" accessibleName={uploadLabel}
          displayValue={new Intl.NumberFormat(i18n.language, { style: "percent" }).format(progress / 100)} />
        {progress === 100 ? <FlexBox alignItems="Center" className="catalogImportActions" role="status">
          <BusyIndicator active delay={0} size="S" />
          <Text>{processingLabel}</Text>
        </FlexBox> : null}
      </FlexBox> : null}
      {operation === "download" ? <BusyIndicator active delay={0} aria-label={t("catalogImport.downloading")} /> : null}
      {error ? <MessageStrip design="Negative" hideCloseButton>{error}</MessageStrip> : null}
      {preview ? <ImportPreview preview={preview} /> : null}
    </FlexBox>
  </Dialog>, document.body);
}
