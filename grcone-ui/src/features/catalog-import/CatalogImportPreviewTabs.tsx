import { useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Button, FlexBox, ObjectStatus, Tab, TabContainer, Table, TableCell, TableHeaderCell,
  TableHeaderRow, TableRow, Text, Tree, TreeItemCustom,
} from "@ui5/webcomponents-react";
import { formatPersianDate } from "@/shared/utils/date.utils";
import type { CatalogImportPreview } from "./catalogImport.api";
import {
  makeImportForest, pageImportTree, pathToImportRow, visibleImportNodes, type PageTreeNode,
} from "./catalogImportTree";
import "./catalog-import.css";

type ResultTab = "preview" | "errors";
const FLAT_COLUMNS = ["ROW", "CODE", "TITLE", "VALID_FROM", "VALID_TO"] as const;

function usePageSizes(tab: ResultTab) {
  const ref = useRef<HTMLDivElement>(null);
  const [pageSizes, setPageSizes] = useState({ preview: 1, errors: 1 });
  const measuredRows = useRef(new Map<ResultTab, { width: number; height: number }>());
  useEffect(() => {
    const frame = ref.current;
    const content = frame?.closest<HTMLElement>(".workbookImportContent");
    const dialog = content?.closest<HTMLElement>("ui5-dialog");
    if (!frame || !content || !dialog) return;
    const header = dialog.querySelector<HTMLElement>(":scope > [slot='header']");
    const footer = dialog.querySelector<HTMLElement>(":scope > [slot='footer']");
    let animation = 0;
    const measure = () => {
      const { height, width } = frame.getBoundingClientRect();
      const renderedRows = Number(frame.dataset.renderedRows);
      if (!height || !renderedRows) return;
      const rootFontSize = parseFloat(getComputedStyle(document.documentElement).fontSize);
      const heightLimit = parseFloat(getComputedStyle(dialog).maxHeight);
      if (!Number.isFinite(heightLimit)) return;
      const table = frame.querySelector("ui5-table");
      const tableHeader = table?.querySelector("ui5-table-header-row")?.getBoundingClientRect().height ?? 0;
      const previous = measuredRows.current.get(tab);
      // Keep space for wrapped rows encountered on other pages at this width.
      const rowHeight = Math.max(3 * rootFontSize, (height - tableHeader) / renderedRows,
        previous?.width === width ? previous.height : 0,
        ...Array.from(table?.querySelectorAll("ui5-table-row") ?? [], (row) => row.getBoundingClientRect().height));
      measuredRows.current.set(tab, { width, height: rowHeight });
      const chrome = content.getBoundingClientRect().height - height + tableHeader
        + (header?.getBoundingClientRect().height ?? 0) + (footer?.getBoundingClientRect().height ?? 0)
        + 2 * rootFontSize + 4;
      const fittedRows = Math.max(1, Math.min(20, Math.floor((heightLimit - chrome) / rowHeight)));
      setPageSizes((current) => current[tab] === fittedRows ? current : { ...current, [tab]: fittedRows });
    };
    const schedule = () => {
      cancelAnimationFrame(animation);
      animation = requestAnimationFrame(measure);
    };
    const observer = new ResizeObserver(schedule);
    [frame, content, header, footer].forEach((element) => { if (element) observer.observe(element); });
    window.addEventListener("resize", schedule);
    window.visualViewport?.addEventListener("resize", schedule);
    schedule();
    return () => {
      cancelAnimationFrame(animation);
      observer.disconnect();
      window.removeEventListener("resize", schedule);
      window.visualViewport?.removeEventListener("resize", schedule);
    };
  }, [tab]);
  return { ref, pageSizes };
}

function pageAt(offset: number, total: number, size: number) {
  const pages = Math.max(1, Math.ceil(total / size));
  const page = Math.min(Math.floor(offset / size), pages - 1);
  return { page, pages, first: page * size, end: (page + 1) * size };
}

function ImportPagination({ page, pages, onPage }: { page: number; pages: number; onPage: (index: number) => void }) {
  const { t, i18n } = useTranslation();
  if (pages === 1) return null;
  const number = new Intl.NumberFormat(i18n.language);
  return <FlexBox alignItems="Center" justifyContent="Center" className="workbookImportActions">
    <Button disabled={page === 0} onClick={() => onPage(page - 1)}>{t("catalogImport.previous")}</Button>
    <Text role="status">{t("catalogImport.page", { page: number.format(page + 1), pages: number.format(pages) })}</Text>
    <Button disabled={page + 1 === pages} onClick={() => onPage(page + 1)}>{t("catalogImport.next")}</Button>
  </FlexBox>;
}

function ImportTreeItem({ item, expanded, target }: {
  item: PageTreeNode; expanded: ReadonlySet<string>; target: number | null;
}) {
  const { t, i18n } = useTranslation();
  const { node, parent } = item;
  const row = node.row;
  const title = row?.title ?? node.existingParent?.title ?? t("catalogImport.unresolved");
  const code = row?.code ?? node.existingParent?.code;
  const kind = row?.kind ?? node.existingParent?.kind;
  const parentTitle = parent && (parent.row?.title || parent.existingParent?.title || t("catalogImport.unresolved"));
  return <TreeItemCustom data-import-node={node.key} selected={row != null && row.rowNumber === target}
    hasChildren={node.children.length > 0} expanded={expanded.has(node.key)}
    content={<FlexBox alignItems="Center" className="catalogImportTreeRow">
      <Text className="catalogImportTreeTitle" maxLines={1} title={title}>{title}</Text>
      {code ? <Text dir="ltr" className="catalogImportTreeCode" maxLines={1} title={code}>{code}</Text> : null}
      {row || node.existingParent ? <Text className="catalogImportTreeDetails">
        {row
          ? t("catalogImport.row", { row: new Intl.NumberFormat(i18n.language).format(row.rowNumber) })
          : t("catalogImport.existingParent")}
      </Text> : null}
      {kind ? <Text className="catalogImportTreeDetails" maxLines={1}>
        {t("catalogImport.kinds." + kind, { defaultValue: kind })}
      </Text> : null}
      {row && !row.valid ? <ObjectStatus state="Negative">{t("catalogImport.rowInvalid")}</ObjectStatus> : null}
      {parentTitle ? <Text className="catalogImportTreeDetails" maxLines={1} title={parentTitle}>
        {t("catalogImport.continuedParent", { parent: parentTitle })}
      </Text> : null}
    </FlexBox>}>
    {item.children.map((child) => <ImportTreeItem key={child.node.key} item={child} expanded={expanded} target={target} />)}
  </TreeItemCustom>;
}

export function CatalogImportPreviewTabs({ preview }: { preview: CatalogImportPreview }) {
  const { t, i18n } = useTranslation();
  const [tab, setTab] = useState<ResultTab>(() => preview.rows.length ? "preview" : "errors");
  const [offset, setOffset] = useState({ preview: 0, errors: 0 });
  const [expanded, setExpanded] = useState<ReadonlySet<string>>(() => new Set());
  const [target, setTarget] = useState<number | null>(null);
  const flat = preview.family === "CONTROL_OBJECTIVE";
  const roots = useMemo(() => flat ? [] : makeImportForest(preview.rows), [flat, preview.rows]);
  const visible = useMemo(() => visibleImportNodes(roots, expanded), [roots, expanded]);
  const count = tab === "errors" ? preview.issues.length : flat ? preview.rows.length : visible.length;
  const { ref, pageSizes } = usePageSizes(tab);
  const previewPage = pageAt(offset.preview, flat ? preview.rows.length : visible.length, pageSizes.preview);
  const errorPage = pageAt(offset.errors, preview.issues.length, pageSizes.errors);
  const currentPage = tab === "preview" ? previewPage : errorPage;
  const renderedRows = Math.max(0, Math.min(count, currentPage.end) - currentPage.first);
  const numbers = new Intl.NumberFormat(i18n.language);
  const rowNumbers = useMemo(() => new Set(preview.rows.map((row) => row.rowNumber)), [preview.rows]);

  const moveToRow = (rowNumber: number) => {
    let index = preview.rows.findIndex((row) => row.rowNumber === rowNumber);
    if (!flat) {
      const path = pathToImportRow(roots, rowNumber);
      if (!path) return;
      const next = new Set(expanded);
      path.slice(0, -1).forEach((node) => next.add(node.key));
      index = visibleImportNodes(roots, next).findIndex(({ node }) => node.row?.rowNumber === rowNumber);
      setExpanded(next);
    }
    if (index < 0) return;
    setOffset((current) => ({ ...current, preview: index }));
    setTarget(rowNumber);
    setTab("preview");
  };

  return <FlexBox direction="Column" className="catalogImportResults">
    <ObjectStatus state={preview.valid ? "Positive" : "Negative"}>
      {preview.valid
        ? t("catalogImport.ready", { total: numbers.format(preview.rows.length) })
        : t("catalogImport.invalid", { total: numbers.format(preview.issues.length) })}
    </ObjectStatus>
    <TabContainer tabLayout="Inline" onTabSelect={(event) =>
      setTab(event.detail.tab.getAttribute("data-result-tab") === "errors" ? "errors" : "preview")}>
      <Tab data-result-tab="preview" text={t("catalogImport.preview")} selected={tab === "preview"}>
        {tab === "preview" ? <FlexBox direction="Column" className="catalogImportResults">
          <FlexBox ref={ref} direction="Column" data-rendered-rows={renderedRows} className="catalogImportPageRows">
            {!preview.rows.length ? <Text>{t("common.noData")}</Text>
              : flat ? <Table accessibleName={t("catalogImport.preview")} overflowMode="Popin"
                headerRow={<TableHeaderRow>{FLAT_COLUMNS.map((column) =>
                  <TableHeaderCell key={column}>{t("catalogImport.fields." + column)}</TableHeaderCell>)}</TableHeaderRow>}>
                {preview.rows.slice(previewPage.first, previewPage.end).map((row) => <TableRow key={row.rowNumber}>
                  <TableCell>
                    <ObjectStatus state={row.valid ? "None" : "Negative"}>{numbers.format(row.rowNumber)}</ObjectStatus>
                  </TableCell>
                  <TableCell><Text maxLines={1} title={row.code}>{row.code}</Text></TableCell>
                  <TableCell><Text maxLines={1} title={row.title}>{row.title}</Text></TableCell>
                  <TableCell>{row.validFrom ? formatPersianDate(row.validFrom) : "—"}</TableCell>
                  <TableCell>{row.validTo ? formatPersianDate(row.validTo) : "—"}</TableCell>
                </TableRow>)}
              </Table> : <Tree className="catalogImportTree" accessibleName={t("catalogImport.preview")} onItemToggle={(event) => {
                event.preventDefault();
                const key = event.detail.item.dataset.importNode;
                if (!key) return;
                setExpanded((current) => {
                  const next = new Set(current);
                  if (!next.delete(key)) next.add(key);
                  return next;
                });
              }}>
                {pageImportTree(visible.slice(previewPage.first, previewPage.end)).map((item) =>
                  <ImportTreeItem key={item.node.key} item={item} expanded={expanded} target={target} />)}
              </Tree>}
          </FlexBox>
          <ImportPagination page={previewPage.page} pages={previewPage.pages}
            onPage={(page) => setOffset((current) => ({ ...current, preview: page * pageSizes.preview }))} />
        </FlexBox> : null}
      </Tab>
      <Tab data-result-tab="errors" text={t("catalogImport.errors")} additionalText={numbers.format(preview.issues.length)}
        selected={tab === "errors"}>
        {tab === "errors" ? <FlexBox direction="Column" className="catalogImportResults">
          <FlexBox ref={ref} direction="Column" data-rendered-rows={renderedRows} className="catalogImportPageRows">
            {preview.issues.length ? <Table accessibleName={t("catalogImport.errors")} overflowMode="Popin"
              headerRow={<TableHeaderRow>
                <TableHeaderCell width="5.5rem">{t("catalogImport.fields.ROW")}</TableHeaderCell>
                <TableHeaderCell width="24%">{t("catalogImport.column")}</TableHeaderCell>
                <TableHeaderCell>{t("catalogImport.problem")}</TableHeaderCell>
              </TableHeaderRow>}>
              {preview.issues.slice(errorPage.first, errorPage.end).map((issue, index) => <TableRow key={errorPage.first + index}>
                <TableCell>{rowNumbers.has(issue.rowNumber)
                  ? <Button design="Transparent" accessibleName={t("catalogImport.goToRow", { row: numbers.format(issue.rowNumber) })}
                    onClick={() => moveToRow(issue.rowNumber)}>{numbers.format(issue.rowNumber)}</Button>
                  : issue.rowNumber > 0 ? numbers.format(issue.rowNumber) : "—"}</TableCell>
                <TableCell>{t("catalogImport.fields." + issue.field, { defaultValue: issue.field })}</TableCell>
                <TableCell>{t("catalogImport.issues." + issue.code, { defaultValue: t("catalogImport.failed") })}</TableCell>
              </TableRow>)}
            </Table> : <Text>{t("catalogImport.noErrors")}</Text>}
          </FlexBox>
          <ImportPagination page={errorPage.page} pages={errorPage.pages}
            onPage={(page) => setOffset((current) => ({ ...current, errors: page * pageSizes.errors }))} />
        </FlexBox> : null}
      </Tab>
    </TabContainer>
  </FlexBox>;
}
