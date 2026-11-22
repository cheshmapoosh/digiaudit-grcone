import type { CSSProperties } from "react";
import { useTranslation } from "react-i18next";
import { CatalogCreateMenu } from "@/features/catalog-import/CatalogCreateMenu";
import type { ProcessCreateType } from "../domain/processControl.model";

export interface ProcessCreateMenuProps {
  disabled?: boolean;
  slot?: string;
  style?: CSSProperties;
  nodeTypes?: ProcessCreateType[];
  onCreate: (value: ProcessCreateType) => void;
}

export default function ProcessCreateMenu({ nodeTypes = ["PROCESS", "SUBPROCESS", "CONTROL"], ...props }: ProcessCreateMenuProps) {
  const { t } = useTranslation();
  return <CatalogCreateMenu<ProcessCreateType> {...props} family="PROCESS" options={[
      { value: "PROCESS", label: t("process.nodeType.process"), disabled: !nodeTypes.includes("PROCESS") },
      { value: "SUBPROCESS", label: t("process.nodeType.subProcess"), disabled: !nodeTypes.includes("SUBPROCESS") },
      { value: "CONTROL", label: t("control.nodeType.control"), disabled: !nodeTypes.includes("CONTROL") },
    ]} />;
}
