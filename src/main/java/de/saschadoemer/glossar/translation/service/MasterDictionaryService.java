package de.saschadoemer.glossar.translation.service;

import de.saschadoemer.glossar.translation.model.DictionaryEntry;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service to load the master dictionary from Excel.
 */
@Service
public class MasterDictionaryService {

    private static final Logger logger = LoggerFactory.getLogger(MasterDictionaryService.class);
    private static final String DEFAULT_FILE_PATH = "/master-dictonary.xlsx";
    private static final Pattern LANGUAGE_CODE_PATTERN = Pattern.compile("^[a-z]{2}-[A-Z]{2}$");

    private Map<String, List<DictionaryEntry>> masterDictionary = new HashMap<>();

    /**
     * Set the master dictionary from an Excel file stream.
     *
     * @param inputStream The Excel file stream.
     * @throws IOException If an error occurs during reading.
     */
    public void setMasterDictionary(InputStream inputStream) throws IOException {
        Map<String, List<DictionaryEntry>> newDictionary = new HashMap<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) continue;

                Map<Integer, String> languageCols = new HashMap<>();
                for (Cell cell : headerRow) {
                    if (cell.getCellType() == CellType.STRING) {
                        String headerValue = cell.getStringCellValue();
                        if (LANGUAGE_CODE_PATTERN.matcher(headerValue).matches() || "de-DE".equalsIgnoreCase(headerValue)) {
                            languageCols.put(cell.getColumnIndex(), headerValue);
                        }
                    }
                }

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    String identifier = getCellValueAsString(row.getCell(0));
                    String german = getCellValueAsString(row.getCell(1));

                    if (identifier != null && !identifier.isEmpty()) {
                        Map<String, String> translations = new HashMap<>();
                        for (Map.Entry<Integer, String> entry : languageCols.entrySet()) {
                            translations.put(entry.getValue(), getCellValueAsString(row.getCell(entry.getKey())));
                        }

                        DictionaryEntry entry = new DictionaryEntry(identifier, german, translations);
                        newDictionary.computeIfAbsent(identifier, k -> new ArrayList<>()).add(entry);
                        if (german != null && !german.isEmpty() && !german.equals(identifier)) {
                            newDictionary.computeIfAbsent(german, k -> new ArrayList<>()).add(entry);
                        }
                    }
                }
            }
        }
        this.masterDictionary = newDictionary;
        logger.info("Uploaded and loaded {} entries into master dictionary.", masterDictionary.size());
    }

    /**
     * Loads entries from the master dictionary.
     *
     * @param targetLanguageCode The language code to filter for.
     * @return A map of term/identifier to list of DictionaryEntry with the specific target language.
     */
    public Map<String, List<DictionaryEntry>> load(String targetLanguageCode) {
        if (masterDictionary.isEmpty()) {
            logger.warn("Master dictionary is empty. Trying to load default one.");
            try (InputStream inputStream = getClass().getResourceAsStream(DEFAULT_FILE_PATH)) {
                if (inputStream != null) {
                    setMasterDictionary(inputStream);
                } else {
                    logger.error("Default master dictionary not found: {}", DEFAULT_FILE_PATH);
                    return Collections.emptyMap();
                }
            } catch (IOException e) {
                logger.error("Error loading default master dictionary", e);
                return Collections.emptyMap();
            }
        }

        Map<String, List<DictionaryEntry>> filteredDictionary = new HashMap<>();
        for (Map.Entry<String, List<DictionaryEntry>> entry : masterDictionary.entrySet()) {
            List<DictionaryEntry> entries = entry.getValue();
            List<DictionaryEntry> filteredEntries = new ArrayList<>();
            for (DictionaryEntry de : entries) {
                // Return a view/copy that makes it compatible with the previous API expectations if needed,
                // or just return the entry as is. Since we changed DictionaryEntry, we just return it.
                filteredEntries.add(de);
            }
            filteredDictionary.put(entry.getKey(), filteredEntries);
        }

        logger.info("Providing {} entries from master dictionary for language: {}", filteredDictionary.size(), targetLanguageCode);
        return filteredDictionary;
    }

    public boolean isMasterDictionarySet() {
        return !masterDictionary.isEmpty();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        try {
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
                case FORMULA -> {
                    try {
                        yield cell.getStringCellValue();
                    } catch (Exception e) {
                        yield String.valueOf(cell.getNumericCellValue());
                    }
                }
                default -> "";
            };
        } catch (Exception e) {
            return "";
        }
    }
}
