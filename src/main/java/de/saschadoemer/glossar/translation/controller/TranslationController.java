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
