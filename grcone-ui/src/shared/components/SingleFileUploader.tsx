import type { ReactNode } from "react";
import { FileUploader } from "@ui5/webcomponents-react";

export function SingleFileUploader({ accept, disabled, accessibleName, onSelect, children }: {
  accept?: string;
  disabled?: boolean;
  accessibleName: string;
  onSelect: (file: File) => void;
  children: ReactNode;
}) {
  return <FileUploader hideInput multiple={false} accept={accept} disabled={disabled} accessibleName={accessibleName}
    onChange={(event) => {
      const uploader = event.currentTarget;
      if (!uploader) return;
      const file = event.detail.files?.[0] ?? uploader.files?.[0];
      uploader.value = "";
      if (file) onSelect(file);
    }}>
    {children}
  </FileUploader>;
}
