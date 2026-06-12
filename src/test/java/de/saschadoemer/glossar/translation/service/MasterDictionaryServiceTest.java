package de.saschadoemer.glossar.translation.service;

import de.saschadoemer.glossar.translation.model.DictionaryEntry;
import de.saschadoemer.glossar.translation.repository.DictionaryEntryRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class MasterDictionaryServiceTest {

    private MasterDictionaryService masterDictionaryService;
    private DictionaryEntryRepository dictionaryEntryRepository;

    @BeforeEach
    void setUp() {
        dictionaryEntryRepository = mock(DictionaryEntryRepository.class);
        masterDictionaryService = new MasterDictionaryService(dictionaryEntryRepository);
    }

    @Test
    void testSetMasterDictionaryWithValidLanguages() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Test");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("de-DE");
        header.createCell(2).setCellValue("en-US");
        header.createCell(3).setCellValue("fr-FR");
        header.createCell(4).setCellValue("InvalidHeader");

        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("TERM1");
        data.createCell(1).setCellValue("Begriff 1");
        data.createCell(2).setCellValue("Term 1");
        data.createCell(3).setCellValue("Terme 1");
        data.createCell(4).setCellValue("Should be ignored");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        byte[] bytes = bos.toByteArray();

        // We need to stub the repository to return something when findAll is called
        // In the service, setMasterDictionary calls saveAll, so we can capture it
        doAnswer(invocation -> {
            List<DictionaryEntry> entries = invocation.getArgument(0);
            when(dictionaryEntryRepository.findAll()).thenReturn(entries);
            when(dictionaryEntryRepository.count()).thenReturn((long) entries.size());
            return entries;
        }).when(dictionaryEntryRepository).saveAll(anyList());

        masterDictionaryService.setMasterDictionary(new ByteArrayInputStream(bytes));

        assertTrue(masterDictionaryService.isMasterDictionarySet());
        List<String> importedLanguages = masterDictionaryService.setMasterDictionary(new ByteArrayInputStream(bytes));
        assertTrue(importedLanguages.contains("en-US"));
        assertTrue(importedLanguages.contains("fr-FR"));
        assertTrue(importedLanguages.contains("de-DE"));
        assertFalse(importedLanguages.contains("InvalidHeader"));
        Map<String, List<DictionaryEntry>> loaded = masterDictionaryService.load("en-US");
        
        assertTrue(loaded.containsKey("TERM1"));
        DictionaryEntry entry = loaded.get("TERM1").getFirst();
        assertEquals("Begriff 1", entry.getGerman());
        assertEquals("Term 1", entry.getTranslation("en-US"));
        assertEquals("Terme 1", entry.getTranslation("fr-FR"));
        assertNull(entry.getTranslation("InvalidHeader"));
    }

    @Test
    void testIsMasterDictionarySetInitialState() {
        assertFalse(masterDictionaryService.isMasterDictionarySet());
    }
}
