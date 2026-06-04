package de.saschadoemer.glossar.translation.service;

import de.saschadoemer.glossar.translation.model.TranslationResult;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service to export translation results.
 */
@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    /**
     * Exports translation results to an Excel byte array.
     *
     * @param results The results to export.
     * @return The Excel file as a byte array.
     */
    public byte[] exportToExcel(List<TranslationResult> results) {
        log.info("Exporting {} results to memory", results.size());

        try (var workbook = new XSSFWorkbook();
             var bos = new java.io.ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Glossary Results");

            var headerRow = sheet.createRow(0);
            String[] headers = {
                    "Content",
                    "Translation with Context",
                    "Confidence",
                    "Translation without Context",
                    "Synonyms",
                    "Comments",
                    "Cost",
                    "Duration (ms)"
            };

            var headerStyle = workbook.createCellStyle();
            var headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (var i = 0; i < headers.length; i++) {
                var cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            var rowNum = 1;
            for (var result : results) {
                if (result == null) continue;
                var row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(safeString(result.getContent()));
                row.createCell(1).setCellValue(safeString(result.getTranslationWithContext()));
                if (result.getConfidence() != null) {
                    row.createCell(2).setCellValue(result.getConfidence());
                }
                row.createCell(3).setCellValue(safeString(result.getTranslationWithoutContext()));
                row.createCell(4).setCellValue(result.getSynonyms() != null ? String.join(", ", result.getSynonyms()) : "");
                row.createCell(5).setCellValue(safeString(result.getComments()));
                if (result.getCost() != null) {
                    row.createCell(6).setCellValue(result.getCost());
                }
                if (result.getDurationMs() != null) {
                    row.createCell(7).setCellValue(result.getDurationMs());
                }
            }

            for (var i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(bos);
            log.info("Export finished successfully.");
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("Error during Excel export", e);
            return new byte[0];
        }
    }

    /**
     * Safely returns a string value, returning an empty string if the input is null.
     *
     * @param value The value to safely convert to a string.
     * @return The string value or an empty string.
     */
    private String safeString(String value) {
        return value == null ? "" : value;
    }

}
