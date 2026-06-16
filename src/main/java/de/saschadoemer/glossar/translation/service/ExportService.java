package de.saschadoemer.glossar.translation.service;

import de.saschadoemer.glossar.translation.model.TranslationResult;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
                    "Input Tokens",
                    "Output Tokens",
                    "Total Tokens",
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
                if (result.getInputTokens() != null) {
                    row.createCell(6).setCellValue(result.getInputTokens());
                }
                if (result.getOutputTokens() != null) {
                    row.createCell(7).setCellValue(result.getOutputTokens());
                }
                if (result.getTotalTokens() != null) {
                    row.createCell(8).setCellValue(result.getTotalTokens());
                }
                if (result.getDurationMs() != null) {
                    row.createCell(9).setCellValue(result.getDurationMs());
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
     * Reads translation results from an Excel byte array.
     *
     * @param excelData The Excel file as a byte array.
     * @return The list of translation results.
     */
    public List<TranslationResult> readFromExcel(byte[] excelData) {
        log.info("Reading results from Excel data ({} bytes)", excelData.length);
        var results = new ArrayList<TranslationResult>();

        try (var bis = new ByteArrayInputStream(excelData);
             var workbook = new XSSFWorkbook(bis)) {
            var sheet = workbook.getSheetAt(0);
            var rowIterator = sheet.iterator();

            // Skip header row
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            while (rowIterator.hasNext()) {
                var row = rowIterator.next();
                var result = new TranslationResult();

                result.setContent(getCellValue(row, 0));
                result.setTranslationWithContext(getCellValue(row, 1));
                result.setConfidence(getCellDoubleValue(row, 2));
                result.setTranslationWithoutContext(getCellValue(row, 3));
                var synonymsText = getCellValue(row, 4);
                if (!synonymsText.isEmpty()) {
                    result.setSynonyms(Arrays.asList(synonymsText.split(", ")));
                }
                result.setComments(getCellValue(row, 5));
                result.setInputTokens(getCellIntValue(row, 6));
                result.setOutputTokens(getCellIntValue(row, 7));
                result.setTotalTokens(getCellIntValue(row, 8));
                result.setDurationMs(getCellLongValue(row, 9));

                results.add(result);
            }
            log.info("Successfully read {} results from Excel.", results.size());
        } catch (Exception e) {
            log.error("Error reading from Excel", e);
        }

        return results;
    }

    private String getCellValue(Row row, int cellNum) {
        var cell = row.getCell(cellNum);
        if (cell == null) return "";
        return cell.toString();
    }

    private Double getCellDoubleValue(Row row, int cellNum) {
        var cell = row.getCell(cellNum);
        if (cell == null) return null;
        try {
            return cell.getNumericCellValue();
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getCellIntValue(Row row, int cellNum) {
        var cell = row.getCell(cellNum);
        if (cell == null) return null;
        try {
            return (int) cell.getNumericCellValue();
        } catch (Exception e) {
            return null;
        }
    }

    private Long getCellLongValue(Row row, int cellNum) {
        var cell = row.getCell(cellNum);
        if (cell == null) return null;
        try {
            return (long) cell.getNumericCellValue();
        } catch (Exception e) {
            return null;
        }
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

}
