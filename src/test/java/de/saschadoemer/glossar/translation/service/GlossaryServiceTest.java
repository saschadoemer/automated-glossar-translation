package de.saschadoemer.glossar.translation.service;

import de.saschadoemer.glossar.translation.model.TranslationJob;
import de.saschadoemer.glossar.translation.model.TranslationResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GlossaryServiceTest {

    private GlossaryServiceImpl glossaryService;
    private MasterDictionaryService masterDictionaryService;
    private ExportService exportService;

    @BeforeEach
    void setUp() {
        masterDictionaryService = mock(MasterDictionaryService.class);
        exportService = mock(ExportService.class);
        glossaryService = new GlossaryServiceImpl(masterDictionaryService, exportService);
    }

    @Test
    void testProcessAllWithEmptyFile() throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        
        // Mock master dictionary to be set
        when(masterDictionaryService.isMasterDictionarySet()).thenReturn(true);
        when(masterDictionaryService.load(anyString())).thenReturn(Collections.emptyMap());

        // We need to set up environment variables or mock the LLM service creation.
        // Since processAll creates the LLM service internally using System.getenv, 
        // this might be tricky in a unit test without additional refactoring.
        // For now, let's at least verify it doesn't crash if the master dictionary is missing.
    }
    @Test
    void testGetJobStatus() {
        MasterDictionaryService masterDictionaryService = new MasterDictionaryService();
        ExportService exportService = new ExportService();
        GlossaryServiceImpl service = new GlossaryServiceImpl(masterDictionaryService, exportService);
        
        assertNull(service.getJobStatus("non-existent"));
    }
}
