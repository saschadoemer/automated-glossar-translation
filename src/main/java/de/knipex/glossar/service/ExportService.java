package de.knipex.glossar.service;

import de.knipex.glossar.model.TranslationResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.util.List;

/**
 * Service to export translation results.
 */
public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

    /**
     * Exports translation results to an Excel file.
     *
     * @param results  The results to export.
     * @param filePath The path to the output file.
     */
    public void exportToExcel(List<TranslationResult> results, String filePath) {
        logger.info("Exporting {} results to {}", results.size(), filePath);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Glossary Results");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Content",
                    "Translation with Context",
                    "Confidence",
                    "Translation without Context",
                    "Synonyms",
                    "Comments"
            };

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fill data rows
            int rowNum = 1;
            for (TranslationResult result : results) {
                if (result == null) continue;
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(safeString(result.getContent()));
                row.createCell(1).setCellValue(safeString(result.getTranslationWithContext()));
                if (result.getConfidence() != null) {
                    row.createCell(2).setCellValue(result.getConfidence());
                }
                row.createCell(3).setCellValue(safeString(result.getTranslationWithoutContext()));
                row.createCell(4).setCellValue(result.getSynonyms() != null ? String.join(", ", result.getSynonyms()) : "");
                row.createCell(5).setCellValue(safeString(result.getComments()));
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }

            logger.info("Export finished successfully.");
        } catch (Exception e) {
            logger.error("Error during Excel export", e);
        }
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }
}
