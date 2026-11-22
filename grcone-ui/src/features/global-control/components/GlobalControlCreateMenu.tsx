import { useTranslation } from "react-i18next";
import { CatalogCreateMenu } from "@/features/catalog-import/CatalogCreateMenu";

type CreateKind = "group" | "control";

export default function GlobalControlCreateMenu({ disabled, slot, groupEnabled, controlEnabled, onCreate }: {
  disabled: boolean;
  slot?: string;
  groupEnabled: boolean;
  controlEnabled: boolean;
  onCreate: (kind: CreateKind) => void;
}) {
  const { t } = useTranslation();
  return <CatalogCreateMenu<CreateKind> family="GLOBAL_CONTROL" disabled={disabled} slot={slot} onCreate={onCreate}
    options={[
      { value: "group", label: t("globalControl.group.menu"), disabled: !groupEnabled },
      { value: "control", label: t("globalControl.control.menu"), disabled: !controlEnabled },
    ]} />;
}
