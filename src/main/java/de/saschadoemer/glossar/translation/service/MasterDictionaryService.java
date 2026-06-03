package de.saschadoemer.glossar.translation.service;

import de.saschadoemer.glossar.translation.model.DictionaryEntry;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * Service to load the master dictionary from Excel.
 */
@Service
public class MasterDictionaryService {

    private static final Logger logger = LoggerFactory.getLogger(MasterDictionaryService.class);
    private static final String FILE_PATH = "/master-dictonary.xlsx";

    /**
     * Loads entries from the master dictionary Excel file.
     *
     * @param targetLanguageCode The language code to load entries for.
     * @return A map of term/identifier to list of DictionaryEntry.
     */
    public Map<String, List<DictionaryEntry>> load(String targetLanguageCode) {
        Map<String, List<DictionaryEntry>> dictionary = new HashMap<>();
        logger.info("Loading master dictionary for language: {}", targetLanguageCode);

        try (InputStream inputStream = getClass().getResourceAsStream(FILE_PATH)) {
            if (inputStream == null) {
                logger.error("Resource not found: {}", FILE_PATH);
                return dictionary;
            }

            try (Workbook workbook = new XSSFWorkbook(inputStream)) {
                boolean languageFoundInAnySheet = false;
                for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                    Sheet sheet = workbook.getSheetAt(s);
                    Row headerRow = sheet.getRow(0);
                    if (headerRow == null) continue;

                    int targetLangCol = -1;
                    for (Cell cell : headerRow) {
                        if (cell.getCellType() == CellType.STRING) {
                            String headerValue = cell.getStringCellValue();
                            if (targetLanguageCode.equalsIgnoreCase(headerValue)) {
                                targetLangCol = cell.getColumnIndex();
                                languageFoundInAnySheet = true;
                                break;
                            }
                        }
                    }

                    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null) continue;

                        String identifier = getCellValueAsString(row.getCell(0));
                        String german = getCellValueAsString(row.getCell(1));
                        String targetLanguage = targetLangCol != -1 ? getCellValueAsString(row.getCell(targetLangCol)) : "";

                        if (identifier != null && !identifier.isEmpty()) {
                            DictionaryEntry entry = new DictionaryEntry(identifier, german, targetLanguage);
                            dictionary.computeIfAbsent(identifier, k -> new ArrayList<>()).add(entry);
                            if (german != null && !german.isEmpty() && !german.equals(identifier)) {
                                dictionary.computeIfAbsent(german, k -> new ArrayList<>()).add(entry);
                            }
                        }
                    }
                }

                if (!languageFoundInAnySheet) {
                    logger.warn("Language code '{}' not found in any Excel sheet headers. Only loading mandatory columns.", targetLanguageCode);
                }
            }
        } catch (Exception e) {
            logger.error("Error loading master dictionary", e);
        }

        logger.info("Loaded {} entries from master dictionary.", dictionary.size());
        return dictionary;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}
