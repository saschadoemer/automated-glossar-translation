package de.saschadoemer.glossar.translation.service;

import de.saschadoemer.glossar.translation.model.DictionaryEntry;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
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

    private static final Logger log = LoggerFactory.getLogger(MasterDictionaryService.class);
    private static final Pattern LANGUAGE_CODE_PATTERN = Pattern.compile("^[a-z]{2}-[A-Z]{2}$");

    private Map<String, List<DictionaryEntry>> masterDictionary = new HashMap<>();

    /**
     * Set the master dictionary from an Excel file stream.
     *
     * @param inputStream The Excel file stream.
     * @return A list of imported language codes.
     * @throws IOException If an error occurs during reading.
     */
    public List<String> setMasterDictionary(InputStream inputStream) throws IOException {
        var newDictionary = new HashMap<String, List<DictionaryEntry>>();
        var importedLanguages = new HashSet<String>();
        try (var workbook = new XSSFWorkbook(inputStream)) {
            for (var s = 0; s < workbook.getNumberOfSheets(); s++) {
                var sheet = workbook.getSheetAt(s);
                var headerRow = sheet.getRow(0);
                if (headerRow == null) continue;

                var languageCols = new HashMap<Integer, String>();
                for (var cell : headerRow) {
                    if (cell.getCellType() == CellType.STRING) {
                        var headerValue = cell.getStringCellValue();
                        if (LANGUAGE_CODE_PATTERN.matcher(headerValue).matches() || "de-DE".equalsIgnoreCase(headerValue)) {
                            languageCols.put(cell.getColumnIndex(), headerValue);
                            importedLanguages.add(headerValue);
                        }
                    }
                }

                for (var i = 1; i <= sheet.getLastRowNum(); i++) {
                    var row = sheet.getRow(i);
                    if (row == null) continue;

                    var identifier = getCellValueAsString(row.getCell(0));
                    var german = getCellValueAsString(row.getCell(1));

                    if (identifier != null && !identifier.isEmpty()) {
                        var translations = new HashMap<String, String>();
                        for (var entry : languageCols.entrySet()) {
                            translations.put(entry.getValue(), getCellValueAsString(row.getCell(entry.getKey())));
                        }

                        var entry = new DictionaryEntry(identifier, german, translations);
                        newDictionary.computeIfAbsent(identifier, _ -> new ArrayList<>()).add(entry);
                        if (german != null && !german.isEmpty() && !german.equals(identifier)) {
                            newDictionary.computeIfAbsent(german, _ -> new ArrayList<>()).add(entry);
                        }
                    }
                }
            }
        }
        this.masterDictionary = newDictionary;
        log.info("Uploaded and loaded {} entries into master dictionary.", masterDictionary.size());
        var result = new ArrayList<>(importedLanguages);
        Collections.sort(result);
        return result;
    }

    /**
     * Loads entries from the master dictionary.
     *
     * @param targetLanguageCode The language code to filter for.
     * @return A map of term/identifier to list of DictionaryEntry with the specific target language.
     */
    public Map<String, List<DictionaryEntry>> load(String targetLanguageCode) {
        if (masterDictionary.isEmpty()) {
            log.warn("Master dictionary is empty. No translations will be enriched with context.");
            return Collections.emptyMap();
        }

        // Return a copy to avoid external modification of the master dictionary
        var result = new HashMap<String, List<DictionaryEntry>>();
        masterDictionary.forEach((key, value) -> result.put(key, new ArrayList<>(value)));

        log.info("Providing {} entries from master dictionary for language: {}", result.size(), targetLanguageCode);
        return result;
    }

    /**
     * Checks if the master dictionary is currently set.
     *
     * @return true if the dictionary is not empty.
     */
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
