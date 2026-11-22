import type { CatalogImportRow } from "./catalogImport.api";

export interface ImportTreeNode {
  key: string;
  row?: CatalogImportRow;
  existingParent?: { code: string; title: string; kind?: string | null };
  children: ImportTreeNode[];
}

interface VisibleNode {
  node: ImportTreeNode;
  parent: ImportTreeNode | null;
}

export interface PageTreeNode extends VisibleNode {
  children: PageTreeNode[];
}

export function makeImportForest(rows: CatalogImportRow[]) {
  const nodes = new Map(rows.map((row): [number, ImportTreeNode] => [
    row.rowNumber, { key: String(row.rowNumber), row, children: [] },
  ]));
  const roots: ImportTreeNode[] = [];
  const externalParents = new Map<string, ImportTreeNode>();
  const unresolved: ImportTreeNode = { key: "unresolved", children: [] };
  for (const row of rows) {
    const node = nodes.get(row.rowNumber)!;
    if (row.parentRow !== null) {
      const visited = new Set([row.rowNumber]);
      let ancestor: number | null | undefined = row.parentRow;
      while (ancestor != null && !visited.has(ancestor)) {
        visited.add(ancestor);
        ancestor = nodes.get(ancestor)?.row?.parentRow;
      }
      const parent = nodes.get(row.parentRow);
      if (ancestor == null && parent) parent.children.push(node);
      else unresolved.children.push(node);
    } else if (row.existingParentTitle != null) {
      const parentKey = `external-${row.existingParentKind ?? ""}:${row.parentCode}`;
      let external = externalParents.get(parentKey);
      if (!external) {
        external = {
          key: parentKey,
          existingParent: { code: row.parentCode, title: row.existingParentTitle, kind: row.existingParentKind },
          children: [],
        };
        externalParents.set(parentKey, external);
        roots.push(external);
      }
      external.children.push(node);
    } else if (row.parentCode) unresolved.children.push(node);
    else roots.push(node);
  }
  if (unresolved.children.length) roots.push(unresolved);
  return roots;
}

export function visibleImportNodes(roots: ImportTreeNode[], expanded: ReadonlySet<string>) {
  const visible: VisibleNode[] = [];
  const visit = (node: ImportTreeNode, parent: ImportTreeNode | null) => {
    visible.push({ node, parent });
    if (expanded.has(node.key)) node.children.forEach((child) => visit(child, node));
  };
  roots.forEach((root) => visit(root, null));
  return visible;
}

export function pageImportTree(visible: VisibleNode[]) {
  const page = new Map(visible.map((item): [string, PageTreeNode] => [
    item.node.key, { ...item, children: [] },
  ]));
  const roots: PageTreeNode[] = [];
  for (const item of page.values()) {
    const parent = item.parent && page.get(item.parent.key);
    if (parent) {
      parent.children.push(item);
      item.parent = null;
    } else roots.push(item);
  }
  return roots;
}

export function pathToImportRow(nodes: ImportTreeNode[], rowNumber: number): ImportTreeNode[] | null {
  for (const node of nodes) {
    if (node.row?.rowNumber === rowNumber) return [node];
    const path = pathToImportRow(node.children, rowNumber);
    if (path) return [node, ...path];
  }
  return null;
}
