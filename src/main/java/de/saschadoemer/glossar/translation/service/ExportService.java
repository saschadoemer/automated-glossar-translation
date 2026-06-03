package de.saschadoemer.glossar.translation.service;

import de.saschadoemer.glossar.translation.model.TranslationResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    "Language",
                    "Translation with Context",
                    "Confidence",
                    "Translation without Context",
                    "Synonyms",
                    "Comments",
                    "Cost",
                    "Duration (ms)"
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
                row.createCell(1).setCellValue(safeString(result.getLanguage()));
                row.createCell(2).setCellValue(safeString(result.getTranslationWithContext()));
                if (result.getConfidence() != null) {
                    row.createCell(3).setCellValue(result.getConfidence());
                }
                row.createCell(4).setCellValue(safeString(result.getTranslationWithoutContext()));
                row.createCell(5).setCellValue(result.getSynonyms() != null ? String.join(", ", result.getSynonyms()) : "");
                row.createCell(6).setCellValue(safeString(result.getComments()));
                if (result.getCost() != null) {
                    row.createCell(7).setCellValue(result.getCost());
                }
                if (result.getDurationMs() != null) {
                    row.createCell(8).setCellValue(result.getDurationMs());
                }
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

    /**
     * Loads translation results from an Excel file.
     *
     * @param filePath The path to the Excel file.
     * @return The list of translation results.
     */
    public List<TranslationResult> loadFromExcel(String filePath) {
        List<TranslationResult> results = new ArrayList<>();
        File file = new File(filePath);
        if (!file.exists()) {
            return results;
        }

        logger.info("Loading previous results from {}", filePath);
        try (Workbook workbook = new XSSFWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return results;

            Map<String, Integer> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                headerMap.put(cell.getStringCellValue(), cell.getColumnIndex());
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                TranslationResult result = new TranslationResult();
                result.setContent(getCellValue(row, headerMap.get("Content")));
                result.setLanguage(getCellValue(row, headerMap.get("Language")));
                result.setTranslationWithContext(getCellValue(row, headerMap.get("Translation with Context")));
                result.setConfidence(getCellDoubleValue(row, headerMap.get("Confidence")));
                result.setTranslationWithoutContext(getCellValue(row, headerMap.get("Translation without Context")));
                
                String synonymsStr = getCellValue(row, headerMap.get("Synonyms"));
                if (synonymsStr != null && !synonymsStr.isEmpty()) {
                    result.setSynonyms(Arrays.asList(synonymsStr.split(", ")));
                }
                
                result.setComments(getCellValue(row, headerMap.get("Comments")));
                result.setCost(getCellDoubleValue(row, headerMap.get("Cost")));
                result.setDurationMs(getCellLongValue(row, headerMap.get("Duration (ms)")));

                results.add(result);
            }
            logger.info("Loaded {} results from existing file.", results.size());
        } catch (Exception e) {
            logger.warn("Could not load previous results from {}: {}", filePath, e.getMessage());
        }
        return results;
    }

    private String getCellValue(Row row, Integer index) {
        if (index == null) return null;
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf(cell.getNumericCellValue());
        return null;
    }

    private Double getCellDoubleValue(Row row, Integer index) {
        if (index == null) return null;
        Cell cell = row.getCell(index);
        if (cell == null || cell.getCellType() != CellType.NUMERIC) return null;
        return cell.getNumericCellValue();
    }

    private Long getCellLongValue(Row row, Integer index) {
        if (index == null) return null;
        Cell cell = row.getCell(index);
        if (cell == null || cell.getCellType() != CellType.NUMERIC) return null;
        return (long) cell.getNumericCellValue();
    }
}
