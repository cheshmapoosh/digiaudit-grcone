import type { CSSProperties } from "react";
import { useTranslation } from "react-i18next";
import { CatalogCreateMenu } from "@/features/catalog-import/CatalogCreateMenu";
import type { ObjectiveKind } from "../domain/objective.model";

interface Props {
  disabled?: boolean;
  slot?: string;
  style?: CSSProperties;
  canCreateObjective: boolean;
  onCreate: (value: ObjectiveKind) => void;
}

export default function ObjectiveCreateMenu({ canCreateObjective, ...props }: Props) {
  const { t } = useTranslation();
  return <CatalogCreateMenu<ObjectiveKind> {...props} family="OBJECTIVE" options={[
      { value: "STRATEGY", label: t("objective.kind.STRATEGY") },
      { value: "OBJECTIVE", label: t("objective.kind.OBJECTIVE"), disabled: !canCreateObjective,
        tooltip: !canCreateObjective ? t("objective.createSelectStrategy") : undefined },
    ]} />;
}
