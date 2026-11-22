import { useContext, useId, useState, type CSSProperties } from "react";
import { createPortal } from "react-dom";
import { useTranslation } from "react-i18next";
import { Menu, MenuItem } from "@ui5/webcomponents-react";
import { CatalogActionButton } from "@/shared/components/CatalogActionButton";
import { CatalogSelectionContext } from "@/shared/components/catalogSelection.context";
import type { CatalogImportFamily } from "./catalogImport.api";
import { CatalogImportDialog } from "./CatalogImportDialog";
import "./catalog-import.css";

export function CatalogCreateMenu<T extends string>({ disabled, slot, style, family, options, onCreate }: {
  disabled?: boolean;
  slot?: string;
  style?: CSSProperties;
  family: CatalogImportFamily;
  options: ReadonlyArray<{ value: T; label: string; disabled?: boolean; tooltip?: string }>;
  onCreate: (value: T) => void;
}) {
  const { t } = useTranslation();
  const id = useId();
  const catalog = useContext(CatalogSelectionContext);
  const [opener, setOpener] = useState<HTMLElement>();
  const [uploadOpen, setUploadOpen] = useState(false);
  const blocked = disabled || catalog?.busy;

  return <>
    <CatalogActionButton slot={slot} style={style} className="createMenuButton" design="Emphasized"
      disabled={blocked} endIcon="slim-arrow-down"
      accessibilityAttributes={{ hasPopup: "menu", expanded: opener ? "true" : "false" }}
      onClick={(event) => setOpener(event.currentTarget ?? undefined)}>
      {t("common.create")}
    </CatalogActionButton>
    {opener ? createPortal(
      <Menu className="catalogCreateMenu" open opener={opener} placement="Bottom" horizontalAlign="End" onClose={() => setOpener(undefined)}
        onItemClick={(event) => {
          if (blocked) return;
          const selected = event.detail.item.dataset.action;
          setOpener(undefined);
          if (selected === "upload") {
            if (catalog) catalog.requestAction(() => setUploadOpen(true));
            return;
          }
          const option = options.find((entry) => entry.value === selected);
          if (option && !option.disabled) onCreate(option.value);
        }}>
        {options.map((option) => <MenuItem key={option.value} id={id + "-" + option.value}
          data-action={option.value} text={option.label} tooltip={option.tooltip} disabled={option.disabled} />)}
        {catalog ? <MenuItem data-action="upload" text={t("catalogImport.upload")} /> : null}
      </Menu>, document.body) : null}
    {uploadOpen && catalog ? <CatalogImportDialog family={family} onClose={() => setUploadOpen(false)} onImported={catalog.reload} /> : null}
  </>;
}
