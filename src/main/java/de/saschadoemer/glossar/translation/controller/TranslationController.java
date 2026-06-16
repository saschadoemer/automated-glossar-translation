package de.saschadoemer.glossar.translation.controller;

import de.saschadoemer.glossar.translation.model.TranslationJob;
import de.saschadoemer.glossar.translation.service.GlossaryService;
import de.saschadoemer.glossar.translation.service.MasterDictionaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/translation")
@Tag(name = "Translation API", description = "Endpoints for glossary translation and dictionary management")
public class TranslationController {

    private static final Logger log = LoggerFactory.getLogger(TranslationController.class);

    private final GlossaryService glossaryService;
    private final MasterDictionaryService masterDictionaryService;

    public TranslationController(GlossaryService glossaryService, MasterDictionaryService masterDictionaryService) {
        this.glossaryService = glossaryService;
        this.masterDictionaryService = masterDictionaryService;
    }

    /**
     * Starts the translation process for a given CSV file.
     *
     * @param file           The CSV file containing terms to translate.
     * @param targetLanguage The language code to translate the terms into.
     * @param fuzzy          Whether to use fuzzy matching for dictionary lookups.
     * @param threshold      Optional threshold for the number of terms to process.
     * @param waitTime       Time in seconds to wait between LLM calls.
     * @return A response entity with the unique job ID.
     */
    @PostMapping(value = "/start", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Start translation process",
            description = "Uploads a CSV file and starts the translation process asynchronously. Returns a unique job identifier.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Translation process started successfully",
                            content = @Content(schema = @Schema(example = "{\"jobId\": \"550e8400-e29b-41d4-a716-446655440000\"}"))),
                    @ApiResponse(responseCode = "400", description = "Invalid input file"),
                    @ApiResponse(responseCode = "412", description = "Master dictionary not set"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<Map<String, String>> startTranslation(
            @Parameter(description = "CSV file with terms", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "Target language code (e.g., en-US)", required = true) @RequestParam String targetLanguage,
            @Parameter(description = "Enable fuzzy matching") @RequestParam(required = false, defaultValue = "false") boolean fuzzy,
            @Parameter(description = "Limit number of processed entries") @RequestParam(required = false) Integer threshold,
            @Parameter(description = "Wait time between requests in seconds") @RequestParam(required = false, defaultValue = "3") int waitTime) {

        log.info("Start translation request received: targetLanguage={}, fuzzy={}, threshold={}, waitTime={}",
                targetLanguage, fuzzy, threshold, waitTime);

        if (file.isEmpty()) {
            log.warn("Start translation failed: Uploaded file is empty");
            return ResponseEntity.badRequest().build();
        }

        if (!masterDictionaryService.isMasterDictionarySet()) {
            log.warn("Start translation failed: Master dictionary not set");
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        }

        try {
            var fileBytes = file.getBytes();
            var jobId = UUID.randomUUID().toString();

            log.info("Starting asynchronous translation job: jobId={}", jobId);

            // Create job and save input data
            var job = new TranslationJob(jobId, targetLanguage, fuzzy, threshold, waitTime);
            job.setInputData(fileBytes);
            glossaryService.saveJob(job);

            CompletableFuture.runAsync(() -> {
                try (var bais = new ByteArrayInputStream(fileBytes)) {
                    glossaryService.processAll(jobId, bais, targetLanguage, fuzzy, threshold, waitTime);
                } catch (IOException e) {
                    log.error("Error processing translation input for jobId={}", jobId, e);
                    throw new RuntimeException("Error processing translation input", e);
                }
            });

            return ResponseEntity.ok(Map.of("jobId", jobId));
        } catch (IOException e) {
            log.error("Failed to read uploaded file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Fetches the result or status of a translation job.
     *
     * @param jobId The unique identifier of the translation job.
     * @return The Excel file if completed, or progress information.
     */
    @GetMapping("/result/{jobId}")
    @Operation(
            summary = "Get translation result or status",
            description = "Returns the created Excel file if the translation is finished, or information about the progress.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Translation completed, Excel file returned"),
                    @ApiResponse(responseCode = "202", description = "Translation in progress, progress information returned"),
                    @ApiResponse(responseCode = "404", description = "Job not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<?> getTranslationResult(
            @Parameter(description = "The job identifier", required = true) @PathVariable String jobId) {
        log.info("Fetching translation result for jobId={}", jobId);
        var job = glossaryService.getJobStatus(jobId);
        if (job == null) {
            log.warn("Job not found: jobId={}", jobId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: Job not found.");
        }

        if (job.isCompleted()) {
            if (job.getError() != null) {
                log.error("Translation job failed: jobId={}, error={}", jobId, job.getError());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error: Translation job failed: " + job.getError());
            }

            var resultData = job.getResultData();
            if (resultData != null && resultData.length > 0) {
                log.info("Returning completed translation result for jobId={}", jobId);
                var resource = new ByteArrayResource(resultData);
                var filename = "glossary-translation-" + jobId + "-" + job.getTargetLanguage().toLowerCase() + ".xlsx";
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            } else {
                log.error("Result data not found for completed job: jobId={}", jobId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error: Result data not found.");
            }
        } else {
            log.info("Job in progress: jobId={}, processedItems={}/{}", jobId, job.getProcessedItems(), job.getTotalItems());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                    "jobId", job.getId(),
                    "targetLanguage", job.getTargetLanguage(),
                    "totalItems", job.getTotalItems(),
                    "processedItems", job.getProcessedItems(),
                    "status", "IN_PROGRESS"
            ));
        }
    }

    /**
     * Lists all translation jobs that have been created.
     *
     * @return A list of translation jobs.
     */
    @GetMapping("/binaries")
    @Operation(
            summary = "List all translation jobs",
            description = "Returns a list of all translation jobs and their current status.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of translation jobs returned")
            }
    )
    public ResponseEntity<List<TranslationJob>> listAllBinaries() {
        log.info("Listing all translation jobs");
        return ResponseEntity.ok(glossaryService.getAllJobs());
    }

    /**
     * Resumes all translation jobs that are still in progress.
     *
     * @return A response entity indicating that the resume process has started.
     */
    @PostMapping("/binaries/resume-all")
    @Operation(
            summary = "Resume all in-progress translations",
            description = "Finds all translation jobs that are not completed and attempts to resume them from their last processed item.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Resume process started successfully"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<Map<String, String>> resumeAllInProgress() {
        log.info("Resume all in-progress translations request received");
        try {
            glossaryService.resumeAllInProgress();
            return ResponseEntity.ok(Map.of("message", "Resume process started for all in-progress jobs."));
        } catch (Exception e) {
            log.error("Failed to resume in-progress translations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    /**
     * Removes all translation jobs that are still in progress.
     *
     * @return A response entity indicating that the removal process has completed.
     */
    @DeleteMapping("/binaries/in-progress")
    @Operation(
            summary = "Remove all in-progress translations",
            description = "Finds and removes all translation jobs that are not completed.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "In-progress jobs removed successfully"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<Map<String, String>> removeAllInProgress() {
        log.info("Remove all in-progress translations request received");
        try {
            glossaryService.removeAllInProgressJobs();
            return ResponseEntity.ok(Map.of("message", "All in-progress translation jobs have been removed."));
        } catch (Exception e) {
            log.error("Failed to remove in-progress translations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Downloads the final binary for a given translation job ID.
     *
     * @param jobId The unique identifier of the translation job.
     * @return The Excel file binary.
     */
    @GetMapping("/binaries/{jobId}/download")
    @Operation(
            summary = "Download translation result binary",
            description = "Returns the final Excel file for the given job identifier.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Binary file returned"),
                    @ApiResponse(responseCode = "404", description = "Job not found or binary not available"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<?> downloadBinary(
            @Parameter(description = "The job identifier", required = true) @PathVariable String jobId) {
        log.info("Downloading binary for jobId={}", jobId);
        var job = glossaryService.getJobStatus(jobId);
        if (job == null) {
            log.warn("Job not found: jobId={}", jobId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: Job not found.");
        }

        if (job.getResultData() != null && job.getResultData().length > 0) {
            log.info("Returning binary result for jobId={}", jobId);
            var resource = new ByteArrayResource(job.getResultData());
            var filename = "glossary-translation-" + jobId + "-" + job.getTargetLanguage().toLowerCase() + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } else {
            log.warn("Binary not available for jobId={}", jobId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: Binary not available for this job.");
        }
    }

    /**
     * Restarts the translation process for a job that contains errors.
     * All rows within the original excel result will be tried again.
     *
     * @param jobId The unique identifier of the translation job.
     * @return A response entity indicating the process has started.
     */
    @PostMapping("/binaries/{jobId}/restart-errors")
    @Operation(
            summary = "Restart translation for job with errors",
            description = "Identifies jobs with errors and restarts the translation for all rows. The result will be stored in a separate field.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Restart process triggered",
                            content = @Content(schema = @Schema(example = "{\"message\": \"Restart process triggered\", \"jobId\": \"550e8400-e29b-41d4-a716-446655440000\"}"))),
                    @ApiResponse(responseCode = "400", description = "Job does not contain errors"),
                    @ApiResponse(responseCode = "404", description = "Job not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<?> restartErrors(
            @Parameter(description = "The job identifier", required = true) @PathVariable String jobId) {
        log.info("Restart errors requested for jobId={}", jobId);
        var job = glossaryService.getJobStatus(jobId);
        if (job == null) {
            log.warn("Job not found: jobId={}", jobId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: Job not found.");
        }

        if (!glossaryService.jobContainsErrors(jobId)) {
            log.warn("Restart errors rejected: Job does not contain errors: jobId={}", jobId);
            return ResponseEntity.badRequest().body("Error: Job does not contain errors.");
        }

        log.info("Starting asynchronous restart for jobId={}", jobId);
        glossaryService.restartJobWithErrors(jobId);

        return ResponseEntity.ok(Map.of("message", "Restart process triggered", "jobId", jobId));
    }

    /**
     * Downloads the corrected binary for a given translation job ID.
     *
     * @param jobId The unique identifier of the translation job.
     * @return The corrected Excel file binary.
     */
    @GetMapping("/binaries/{jobId}/download-corrected")
    @Operation(
            summary = "Download corrected translation result binary",
            description = "Returns the corrected Excel file for the given job identifier if available.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Binary file returned"),
                    @ApiResponse(responseCode = "404", description = "Job not found or corrected binary not available"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<?> downloadCorrectedBinary(
            @Parameter(description = "The job identifier", required = true) @PathVariable String jobId) {
        log.info("Downloading corrected binary for jobId={}", jobId);
        var job = glossaryService.getJobStatus(jobId);
        if (job == null) {
            log.warn("Job not found: jobId={}", jobId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: Job not found.");
        }

        if (job.getCorrectedResultData() != null && job.getCorrectedResultData().length > 0) {
            log.info("Returning corrected binary result for jobId={}", jobId);
            var resource = new ByteArrayResource(job.getCorrectedResultData());
            var filename = "glossary-translation-corrected-" + jobId + "-" + job.getTargetLanguage().toLowerCase() + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } else {
            log.warn("Corrected binary not available for jobId={}", jobId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: Corrected binary not available for this job.");
        }
    }

    /**
     * Uploads and processes a master dictionary Excel file.
     *
     * @param file The Excel file containing the master dictionary.
     * @return A response entity containing the imported languages summary.
     */
    @PostMapping(value = "/master-dictionary/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload master dictionary",
            description = "Uploads an Excel file to be used as the master dictionary for translations. Returns a summary of imported languages.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Master dictionary uploaded successfully",
                            content = @Content(schema = @Schema(example = "{\"importedLanguages\": [\"de-DE\", \"en-US\"]}"))),
                    @ApiResponse(responseCode = "400", description = "No file selected"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<?> uploadMasterDictionary(
            @Parameter(description = "Excel file with dictionary data", required = true) @RequestParam("file") MultipartFile file) {
        log.info("Master dictionary upload request received");
        if (file.isEmpty()) {
            log.warn("Master dictionary upload failed: No file selected");
            return ResponseEntity.badRequest().body("Please select a file to upload.");
        }

        try {
            var importedLanguages = masterDictionaryService.setMasterDictionary(file.getInputStream());
            log.info("Master dictionary uploaded successfully: languages={}", importedLanguages);
            return ResponseEntity.ok(Map.of("importedLanguages", importedLanguages));
        } catch (IOException e) {
            log.error("Error processing master dictionary upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing master dictionary: " + e.getMessage());
        }
    }
}
