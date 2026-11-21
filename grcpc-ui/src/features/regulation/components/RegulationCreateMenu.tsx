import type { CSSProperties } from "react";
import { useTranslation } from "react-i18next";
import { CatalogCreateMenu } from "@/features/catalog-import/CatalogCreateMenu";

import type { CentralRegulationNodeType } from "../domain/centralRegulation.model";

const ALL_REGULATION_CREATE_TYPES: CentralRegulationNodeType[] = ["GROUP", "REGULATION", "REQUIREMENT"];

interface Props {
  disabled?: boolean;
  slot?: string;
  style?: CSSProperties;
  nodeTypes: CentralRegulationNodeType[];
  onCreate: (nodeType: CentralRegulationNodeType) => void;
}

export default function RegulationCreateMenu({
  disabled = false,
  slot,
  style,
  nodeTypes,
  onCreate,
}: Props) {
  const { t } = useTranslation();
  const labels: Record<CentralRegulationNodeType, string> = {
    GROUP: t("regulation.nodeType.group"),
    REGULATION: t("regulation.nodeType.regulation"),
    REQUIREMENT: t("regulation.nodeType.requirement"),
  };
  return <CatalogCreateMenu
    family="REGULATION"
    disabled={disabled}
    slot={slot}
    style={style}
    options={ALL_REGULATION_CREATE_TYPES.map((type) => ({
      value: type, label: labels[type], disabled: !nodeTypes.includes(type),
    }))}
    onCreate={onCreate}
  />;
}
