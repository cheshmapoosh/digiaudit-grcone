package com.digiaudit.grcpc.modules.masterdata.catalog.regulation.application;

import com.digiaudit.grcpc.modules.masterdata.catalog.regulation.api.dto.RegulationRequirementImportDtos.Issue;
import com.digiaudit.grcpc.modules.masterdata.shared.application.workbook.MasterDataWorkbookReader;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class RegulationRequirementWorkbookReader {
  public static final int MAX_ROWS = MasterDataWorkbookReader.MAX_ROWS;
  public static final int MAX_FILE_BYTES = MasterDataWorkbookReader.MAX_FILE_BYTES;
  private static final MasterDataWorkbookReader.Schema SCHEMA = new MasterDataWorkbookReader.Schema(
      "الزامات", List.of(List.of("عنوان الزام", "شرح الزام", "کد الزام", "ترتیب نمایش", "اعتبار از", "اعتبار تا")),
      List.of("title", "description", "code", "sortOrder", "validFrom", "validTo"), Set.of(), Set.of());

  private final MasterDataWorkbookReader reader;

  public RegulationRequirementWorkbookReader(MasterDataWorkbookReader reader) {
    this.reader = reader;
  }

  public record RawRow(int rowNumber, List<String> cells) {}
  public record Parsed(String digest, List<RawRow> rows, List<Issue> issues) {}

  public Parsed read(MultipartFile file) {
    var parsed = reader.read(file, SCHEMA);
    return new Parsed(parsed.digest(),
        parsed.rows().stream().map(row -> new RawRow(row.rowNumber(), row.cells())).toList(),
        parsed.issues().stream().map(issue -> new Issue(issue.rowNumber(), issue.field(), issue.code())).toList());
  }

  static String digest(byte[] bytes) {
    return MasterDataWorkbookReader.digest(bytes);
  }
}
