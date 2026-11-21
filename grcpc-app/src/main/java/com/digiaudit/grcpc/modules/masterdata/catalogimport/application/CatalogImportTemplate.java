package com.digiaudit.grcpc.modules.masterdata.catalogimport.application;

import com.digiaudit.grcpc.modules.masterdata.catalogimport.application.CatalogImportSchema.Column;
import com.digiaudit.grcpc.modules.masterdata.catalogimport.application.CatalogImportSchema.Family;
import com.digiaudit.grcpc.modules.masterdata.catalogimport.application.CatalogImportSchema.Kind;
import com.digiaudit.grcpc.modules.masterdata.shared.application.workbook.MasterDataWorkbookReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class CatalogImportTemplate {
  private static final String SHEET_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
  private static final String REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
  private static final String PACKAGE_NS = "http://schemas.openxmlformats.org/package/2006/relationships";

  private CatalogImportTemplate() {}

  public static byte[] write(Family family) {
    var headers = family.columns.stream().map(column -> column.fa).toList();
    var guide = new ArrayList<List<String>>();
    guide.add(List.of("برای انتقال ردیف نمونه به برگهٔ داده‌ها از گزینهٔ چسباندن مقادیر استفاده کنید."));
    guide.add(List.of("کد و کد والد را به‌صورت متن وارد کنید. والد می‌تواند در همین فایل یا در پروفایل جاری باشد."));
    guide.add(List.of("تاریخ را به شکل شمسی ۱۴۰۵/۰۱/۰۱ یا میلادی 2026-03-21 به‌صورت متن وارد کنید."));
    guide.add(List.of(family.columns.contains(Column.KIND)
        ? "نوع رکورد را از فهرست کشویی انتخاب کنید."
        : "نوع رکوردهای این الگو هدف کنترلی است."));
    guide.add(headers);
    for (Kind kind : family.kinds) guide.add(family.columns.stream().map(column -> example(family, kind, column)).toList());
    if (family == Family.REGULATION)
      guide.add(List.of("برای الزام، کد قانون مربوط را در ستون کد والد وارد کنید."));
    if (family == Family.ORGANIZATION)
      guide.add(List.of("در ستون نوع سازمان، نام دقیق یک نوع فعال در مدیریت داده‌های اولیهٔ پروفایل جاری را بنویسید."));
    guide.add(List.of("گزینه‌های مجاز ستون‌ها"));
    for (Column column : family.columns) {
      if (column.options != null) guide.add(List.of(column.fa,
          String.join("، ", Arrays.stream(column.options.getEnumConstants()).map(Enum::name).toList())));
    }
    if (family == Family.CONTROL) guide.add(List.of("برای چند حوزهٔ کنترلی، کد گزینه‌ها را با ویرگول جدا کنید."));
    if (family == Family.CONTROL || family == Family.ACCOUNT_GROUP) guide.add(List.of("برای گزینه‌های بله/خیر از بله یا خیر استفاده کنید."));

    var buffer = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
      entry(zip, "[Content_Types].xml", """
          <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
            <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
            <Default Extension="xml" ContentType="application/xml"/>
            <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
            <Override PartName="/xl/worksheets/data.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            <Override PartName="/xl/worksheets/guide.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
          </Types>
          """);
      entry(zip, "_rels/.rels", "<Relationships xmlns=\"" + PACKAGE_NS + "\">"
          + relationship("rId1", "officeDocument", "xl/workbook.xml") + "</Relationships>");
      entry(zip, "xl/workbook.xml", "<workbook xmlns=\"" + SHEET_NS + "\" xmlns:r=\"" + REL_NS
          + "\"><bookViews><workbookView activeTab=\"0\"/></bookViews><sheets>"
          + "<sheet name=\"" + CatalogImportSchema.SHEET + "\" sheetId=\"1\" r:id=\"rId1\"/>"
          + "<sheet name=\"راهنما\" sheetId=\"2\" r:id=\"rId2\"/></sheets></workbook>");
      entry(zip, "xl/_rels/workbook.xml.rels", "<Relationships xmlns=\"" + PACKAGE_NS + "\">"
          + relationship("rId1", "worksheet", "worksheets/data.xml")
          + relationship("rId2", "worksheet", "worksheets/guide.xml")
          + relationship("rId3", "styles", "styles.xml") + "</Relationships>");
      entry(zip, "xl/styles.xml", """
          <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
            <fonts count="2"><font><sz val="11"/><name val="Arial"/></font><font><b/><sz val="11"/><name val="Arial"/></font></fonts>
            <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
            <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
            <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
            <cellXfs count="3">
              <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
              <xf numFmtId="49" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
              <xf numFmtId="49" fontId="1" fillId="0" borderId="0" xfId="0" applyNumberFormat="1" applyFont="1"/>
            </cellXfs>
            <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
          </styleSheet>
          """);
      entry(zip, "xl/worksheets/data.xml", sheet(List.of(headers), headers.size(), 1, recordTypeValidation(family)));
      entry(zip, "xl/worksheets/guide.xml", sheet(guide, Math.max(2, headers.size()), 5, ""));
    } catch (IOException error) {
      throw new UncheckedIOException(error);
    }
    return buffer.toByteArray();
  }

  private static String example(Family family, Kind kind, Column column) {
    return switch (column) {
      case KIND -> kind.fa;
      case CODE -> sampleCode(family, kind);
      case TITLE -> kind.fa + " نمونه";
      case PARENT_CODE -> kind.requiresParent() ? sampleCode(family, kind.parentKind()) : "";
      case ORGANIZATION_TYPE -> kind == Kind.ORGANIZATION ? "نام نوع سازمان فعال" : "";
      case REASONABLE_ASSURANCE -> kind == Kind.ACCOUNT_GROUP ? "خیر" : "";
      case ACCOUNT_IMPORTANCE, POLICY_TYPE, RISK_TYPE ->
          kind.extra.contains(column) ? column.options.getEnumConstants()[0].name() : "";
      default -> "";
    };
  }

  private static String sampleCode(Family family, Kind kind) {
    return "EX-" + (family.kinds.indexOf(kind) + 1);
  }

  private static String relationship(String id, String type, String target) {
    return "<Relationship Id=\"" + id + "\" Type=\"" + REL_NS + "/" + type + "\" Target=\"" + target + "\"/>";
  }

  private static String recordTypeValidation(Family family) {
    int index = family.columns.indexOf(Column.KIND);
    if (index < 0) return "";
    String column = columnName(index);
    String range = column + "2:" + column + MasterDataWorkbookReader.MAX_WORKSHEET_ROW;
    String options = String.join(",", family.kinds.stream().map(kind -> kind.fa).toList());
    return """
        <dataValidations count="1">
          <dataValidation type="list" allowBlank="0" showDropDown="0" showErrorMessage="1" errorStyle="stop"
              errorTitle="نوع رکورد نامعتبر" error="نوع رکورد را از فهرست انتخاب کنید." sqref="%s">
            <formula1>"%s"</formula1>
          </dataValidation>
        </dataValidations>
        """.formatted(range, escapeXml(options));
  }

  private static String sheet(List<List<String>> rows, int columns, int headerRow, String validation) {
    var xml = new StringBuilder("<worksheet xmlns=\"").append(SHEET_NS)
        .append("\"><sheetViews><sheetView workbookViewId=\"0\" rightToLeft=\"1\"/></sheetViews>")
        .append("<cols><col min=\"1\" max=\"").append(columns)
        .append("\" width=\"24\" style=\"1\" customWidth=\"1\"/></cols><sheetData>");
    for (int i = 0; i < rows.size(); i++) {
      int row = i + 1;
      xml.append("<row r=\"").append(row).append("\">");
      for (int j = 0; j < rows.get(i).size(); j++) {
        String value = rows.get(i).get(j);
        if (value.isEmpty()) continue;
        xml.append("<c r=\"").append(columnName(j)).append(row).append("\" t=\"inlineStr\" s=\"")
            .append(row == headerRow ? 2 : 1).append("\"><is><t xml:space=\"preserve\">")
            .append(escapeXml(value))
            .append("</t></is></c>");
      }
      xml.append("</row>");
    }
    return xml.append("</sheetData>").append(validation).append("</worksheet>").toString();
  }

  private static String escapeXml(String value) {
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private static String columnName(int index) {
    var name = new StringBuilder();
    for (int n = index + 1; n > 0; n = (n - 1) / 26)
      name.append((char) ('A' + (n - 1) % 26));
    return name.reverse().toString();
  }

  private static void entry(ZipOutputStream zip, String name, String xml) throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    zip.write(xml.getBytes(StandardCharsets.UTF_8));
    zip.closeEntry();
  }
}
