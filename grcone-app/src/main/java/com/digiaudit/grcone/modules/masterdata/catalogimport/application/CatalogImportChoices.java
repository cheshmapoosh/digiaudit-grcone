package com.digiaudit.grcone.modules.masterdata.catalogimport.application;

import com.digiaudit.grcone.modules.masterdata.catalogimport.application.CatalogImportSchema.Column;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

final class CatalogImportChoices {
  private static final ResourceBundle LABELS = ResourceBundle.getBundle(
      "catalog_import_options", Locale.forLanguageTag("fa"));

  private CatalogImportChoices() {}

  static List<String> forColumn(Column column, List<String> organizationTypes) {
    if (column == Column.ORGANIZATION_TYPE) return organizationTypes;
    if (column.booleanChoice()) return List.of("بله", "خیر");
    if (column.options == null) return List.of();
    return Arrays.stream(column.options.getEnumConstants()).map(option -> label(column, option)).toList();
  }

  static String label(Column column, Enum<?> option) {
    Column primary = column.additionalRelevance() ? Column.CONTROL_RELEVANCE : column;
    String key = primary.name() + "." + option.name();
    return LABELS.containsKey(key) ? LABELS.getString(key) : option.name();
  }

  static Enum<?> resolve(Column column, String value) {
    return Arrays.stream(column.options.getEnumConstants())
        .filter(option -> option.name().equalsIgnoreCase(value) || label(column, option).equals(value))
        .findFirst().orElse(null);
  }
}
