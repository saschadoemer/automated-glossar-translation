package de.saschadoemer.glossar.translation.controller;

import de.saschadoemer.glossar.translation.model.TranslationJob;
import de.saschadoemer.glossar.translation.service.GlossaryService;
import de.saschadoemer.glossar.translation.service.MasterDictionaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TranslationControllerTest {

    private GlossaryService glossaryService;
    private MasterDictionaryService masterDictionaryService;
    private TranslationController controller;

    @BeforeEach
    void setUp() {
        glossaryService = mock(GlossaryService.class);
        masterDictionaryService = mock(MasterDictionaryService.class);
        controller = new TranslationController(glossaryService, masterDictionaryService);
    }

    @Test
    void testListAllBinaries() {
        var jobs = List.of(new TranslationJob("1", "en"), new TranslationJob("2", "de"));
        when(glossaryService.getAllJobs()).thenReturn(jobs);

        ResponseEntity<List<TranslationJob>> response = controller.listAllBinaries();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void testDownloadBinaryNotFound() {
        when(glossaryService.getJobStatus("123")).thenReturn(null);

        ResponseEntity<?> response = controller.downloadBinary("123");
        
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testDownloadBinarySuccess() {
        TranslationJob job = new TranslationJob("123", "en");
        job.setResultData("fake-binary".getBytes());
        job.setCompleted(true);
        when(glossaryService.getJobStatus("123")).thenReturn(job);

        ResponseEntity<?> response = controller.downloadBinary("123");
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("attachment; filename=\"glossary-translation-123-en.xlsx\"", response.getHeaders().getFirst("Content-Disposition"));
    }
}
