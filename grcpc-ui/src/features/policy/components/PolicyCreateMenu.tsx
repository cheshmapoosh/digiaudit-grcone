import type { CSSProperties } from "react";
import { useTranslation } from "react-i18next";
import { CatalogCreateMenu } from "@/features/catalog-import/CatalogCreateMenu";
import type { CentralPolicyNodeType } from "../domain/centralPolicy.model";

interface Props {
  disabled?: boolean;
  slot?: string;
  style?: CSSProperties;
  nodeTypes: CentralPolicyNodeType[];
  onCreate: (value: CentralPolicyNodeType) => void;
}

export default function PolicyCreateMenu({ nodeTypes, ...props }: Props) {
  const { t } = useTranslation();
  return <CatalogCreateMenu<CentralPolicyNodeType> {...props} family="POLICY" options={[
      { value: "GROUP", label: t("policy.nodeType.group"), disabled: !nodeTypes.includes("GROUP") },
      { value: "POLICY", label: t("policy.nodeType.policy"), disabled: !nodeTypes.includes("POLICY") },
    ]} />;
}
