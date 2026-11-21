package com.digiaudit.grcpc.modules.masterdata.catalogimport.api;

import com.digiaudit.grcpc.modules.masterdata.catalogimport.application.CatalogImportSchema.Family;
import com.digiaudit.grcpc.modules.masterdata.catalogimport.application.CatalogImportService;
import com.digiaudit.grcpc.modules.masterdata.catalogimport.application.CatalogImportService.InvalidPreview;
import com.digiaudit.grcpc.modules.masterdata.catalogimport.application.CatalogImportService.Preview;
import com.digiaudit.grcpc.modules.masterdata.catalogimport.application.CatalogImportService.Result;
import com.digiaudit.grcpc.modules.masterdata.catalogimport.application.CatalogImportTemplate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/master-data/catalog-import/{family}")
public class CatalogImportController {
  private final CatalogImportService imports;

  public CatalogImportController(CatalogImportService imports) {
    this.imports = imports;
  }

  @GetMapping("/template")
  @PreAuthorize("@masterDataAuthorization.canManageAny()")
  public ResponseEntity<byte[]> template(@PathVariable Family family) {
    imports.requireAccess(family);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(family.fileName()).build().toString())
        .body(CatalogImportTemplate.write(family));
  }

  @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("@masterDataAuthorization.canManageAny()")
  public Preview preview(@PathVariable Family family, @RequestParam MultipartFile file) {
    return imports.preview(family, file);
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("@masterDataAuthorization.canManageAny()")
  public Result commit(@PathVariable Family family, @RequestParam MultipartFile file, @RequestParam String fingerprint) {
    return imports.commit(family, file, fingerprint);
  }

  public record FailedPreview(String code, Preview preview) {}

  @ExceptionHandler(InvalidPreview.class)
  public ResponseEntity<FailedPreview> rejected(InvalidPreview error) {
    return ResponseEntity.status(error.stale() ? HttpStatus.CONFLICT : HttpStatus.UNPROCESSABLE_ENTITY)
        .body(new FailedPreview(error.stale() ? "CATALOG_IMPORT_STALE" : "CATALOG_IMPORT_INVALID", error.preview()));
  }
}
