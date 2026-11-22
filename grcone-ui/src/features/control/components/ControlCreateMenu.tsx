import type { CSSProperties } from "react";
import { useTranslation } from "react-i18next";
import { CatalogCreateMenu } from "@/features/catalog-import/CatalogCreateMenu";
import type { CentralControlNodeType } from "../domain/centralControl.model";

interface Props {
  disabled?: boolean;
  slot?: string;
  style?: CSSProperties;
  nodeTypes: CentralControlNodeType[];
  onCreate: (value: CentralControlNodeType) => void;
}

export default function ControlCreateMenu({ nodeTypes, ...props }: Props) {
  const { t } = useTranslation();
  return <CatalogCreateMenu<CentralControlNodeType> {...props} family="CONTROL" options={[
      { value: "GROUP", label: t("control.nodeType.group"), disabled: !nodeTypes.includes("GROUP") },
      { value: "CONTROL", label: t("control.nodeType.control"), disabled: !nodeTypes.includes("CONTROL") },
    ]} />;
}
