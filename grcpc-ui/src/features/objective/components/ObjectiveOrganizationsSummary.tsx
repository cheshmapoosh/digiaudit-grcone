import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { BusyIndicator, MessageStrip, Text } from "@ui5/webcomponents-react";
import type { ObjectiveOrganizationLink } from "../domain/objective.model";
import { objectiveApi } from "../infra/objective.api.repo";

export function ObjectiveOrganizationsSummary({ objectiveId }: { objectiveId: string }) {
  const { i18n } = useTranslation();
  const [rows, setRows] = useState<ObjectiveOrganizationLink[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    void objectiveApi.organizations(objectiveId, false, controller.signal).then((links) => {
      if (!controller.signal.aborted) setRows(links);
    }).catch((cause: unknown) => {
      if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : String(cause));
    });
    return () => controller.abort();
  }, [objectiveId]);

  if (error) return <MessageStrip design="Negative" hideCloseButton>{error}</MessageStrip>;
  if (rows === null) return <BusyIndicator active delay={0} size="S" />;
  const names = rows.filter((row) => row.status !== "DELETED").map((row) => row.organizationName);
  return <Text>{names.join(i18n.language.startsWith("fa") ? "، " : ", ") || "—"}</Text>;
}
