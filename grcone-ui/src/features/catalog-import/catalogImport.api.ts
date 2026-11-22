import { httpClient } from "@/shared/infra/http.client";

export type CatalogImportFamily = "ORGANIZATION" | "PROCESS" | "OBJECTIVE" | "CONTROL_OBJECTIVE"
  | "CONTROL" | "GLOBAL_CONTROL" | "RISK" | "REGULATION" | "POLICY" | "ACCOUNT_GROUP";

export interface CatalogImportRow {
  rowNumber: number;
  kind: string;
  code: string;
  title: string;
  parentCode: string;
  parentRow: number | null;
  existingParentTitle: string | null;
  existingParentKind?: string | null;
  validFrom: string | null;
  validTo: string | null;
  valid: boolean;
}

export interface CatalogImportIssue {
  rowNumber: number;
  field: string;
  code: string;
}

export interface CatalogImportPreview {
  family: CatalogImportFamily;
  valid: boolean;
  fingerprint: string | null;
  rows: CatalogImportRow[];
  issues: CatalogImportIssue[];
}

const base = (family: CatalogImportFamily) => "/api/master-data/catalog-import/" + family;

function payload(file: File, fingerprint?: string) {
  const data = new FormData();
  data.append("file", file);
  if (fingerprint) data.append("fingerprint", fingerprint);
  return data;
}

export const catalogImportApi = {
  template: (family: CatalogImportFamily) =>
    httpClient.get<Blob>(base(family) + "/template", { responseType: "blob" }),
  preview: (family: CatalogImportFamily, file: File, signal: AbortSignal, onUploadProgress: (percent: number) => void) =>
    httpClient.post<CatalogImportPreview>(base(family) + "/preview", payload(file), {
      signal, onUploadProgress, successMessage: false,
    }),
  commit: (family: CatalogImportFamily, file: File, fingerprint: string, onUploadProgress: (percent: number) => void) =>
    httpClient.post<{ count: number; revisionId: string }>(base(family), payload(file, fingerprint), {
      onUploadProgress, successMessage: false,
    }),
};
