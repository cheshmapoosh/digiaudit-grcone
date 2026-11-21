import type { CSSProperties } from "react";
import { useTranslation } from "react-i18next";
import { CatalogCreateMenu } from "@/features/catalog-import/CatalogCreateMenu";
import type { CentralRiskCreateKind } from "../domain/centralRisk.model";

interface Props {
  disabled?: boolean;
  slot?: string;
  style?: CSSProperties;
  templateEnabled: boolean;
  onCreate: (value: CentralRiskCreateKind) => void;
}

export default function CentralRiskCreateMenu({ templateEnabled, ...props }: Props) {
  const { t } = useTranslation();
  return <CatalogCreateMenu<CentralRiskCreateKind> {...props} family="RISK" options={[
      { value: "category", label: t("risk.nodeType.category") },
      { value: "template", label: t("risk.nodeType.template"), disabled: !templateEnabled },
    ]} />;
}
