package de.saschadoemer.glossar.translation.service;

import de.saschadoemer.glossar.translation.model.TranslationJob;
import de.saschadoemer.glossar.translation.model.TranslationResult;
import de.saschadoemer.glossar.translation.repository.TranslationJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlossaryCorrectionTest {

    private MasterDictionaryService masterDictionaryService;
    private ExportService exportService;
    private TranslationJobRepository translationJobRepository;
    private GlossaryService glossaryService;

    @BeforeEach
    void setUp() {
        masterDictionaryService = mock(MasterDictionaryService.class);
        exportService = mock(ExportService.class);
        translationJobRepository = mock(TranslationJobRepository.class);
        glossaryService = new GlossaryService(
                masterDictionaryService,
                exportService,
                translationJobRepository,
                "fake-key",
                "fake-model"
        );
    }

    @Test
    void testJobContainsErrors() {
        String jobId = "test-job";
        byte[] excelData = "fake-excel".getBytes();
        TranslationJob job = new TranslationJob(jobId, "en");
        job.setResultData(excelData);

        when(translationJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        TranslationResult resultWithError = new TranslationResult();
        resultWithError.setComments("Error parsing LLM response.");
        
        when(exportService.readFromExcel(excelData)).thenReturn(List.of(resultWithError));

        boolean containsErrors = glossaryService.jobContainsErrors(jobId);
        assertTrue(containsErrors);
    }

    @Test
    void testRestartJobWithErrorsSavesToCorrectedField() {
        String jobId = "test-job";
        byte[] originalExcelData = "original-excel".getBytes();
        byte[] correctedExcelData = "corrected-excel".getBytes();
        
        TranslationJob job = new TranslationJob(jobId, "en", false, null, 0);
        job.setResultData(originalExcelData);

        when(translationJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        TranslationResult errorResult = new TranslationResult();
        errorResult.setContent("Term1");
        errorResult.setComments("Error parsing LLM response.");

        when(exportService.readFromExcel(originalExcelData)).thenReturn(List.of(errorResult));
        when(exportService.exportToExcel(any())).thenReturn(correctedExcelData);

        // We can't easily mock the LLM call inside restartJobWithErrors because LlmService is created with 'new'
        // But we can check if it attempts to save to correctedResultData.
        // To avoid actual LLM calls failing the test (due to fake key), we might need to be careful.
        // Actually, in the current implementation, if LlmService fails to initialize or translate, 
        // it logs an error but continues.
        
        glossaryService.restartJobWithErrors(jobId);

        ArgumentCaptor<TranslationJob> jobCaptor = ArgumentCaptor.forClass(TranslationJob.class);
        verify(translationJobRepository, atLeastOnce()).save(jobCaptor.capture());
        
        TranslationJob savedJob = jobCaptor.getValue();
        assertArrayEquals(correctedExcelData, savedJob.getCorrectedResultData());
    }
}
