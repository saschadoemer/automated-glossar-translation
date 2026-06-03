package de.saschadoemer.glossar.translation.controller;

import de.saschadoemer.glossar.translation.service.GlossaryService;
import de.saschadoemer.glossar.translation.service.MasterDictionaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/translation")
@Tag(name = "Translation API", description = "Endpoints for glossary translation and dictionary management")
public class TranslationController {

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
     * @param llmType        The type of LLM to use (e.g., "gemini" or "openai").
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
            @Parameter(description = "LLM provider type") @RequestParam(required = false, defaultValue = "gemini") String llmType,
            @Parameter(description = "Limit number of processed entries") @RequestParam(required = false) Integer threshold,
            @Parameter(description = "Wait time between requests in seconds") @RequestParam(required = false, defaultValue = "3") int waitTime) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (!masterDictionaryService.isMasterDictionarySet()) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        }

        try {
            var fileBytes = file.getBytes();
            var jobId = UUID.randomUUID().toString();

            CompletableFuture.runAsync(() -> {
                try (var bais = new ByteArrayInputStream(fileBytes)) {
                    glossaryService.processAll(jobId, bais, targetLanguage, fuzzy, llmType, threshold, waitTime);
                } catch (IOException e) {
                    throw new RuntimeException("Error processing translation input", e);
                }
            });

            return ResponseEntity.ok(Map.of("jobId", jobId));
        } catch (IOException e) {
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
        var job = glossaryService.getJobStatus(jobId);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: Job not found.");
        }

        if (job.isCompleted()) {
            if (job.getError() != null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error: Translation job failed: " + job.getError());
            }

            var file = new File(job.getResultFilePath());
            if (file.exists()) {
                Resource resource = new FileSystemResource(file);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error: Result file not found.");
            }
        } else {
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
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload.");
        }

        try {
            var importedLanguages = masterDictionaryService.setMasterDictionary(file.getInputStream());
            return ResponseEntity.ok(Map.of("importedLanguages", importedLanguages));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing master dictionary: " + e.getMessage());
        }
    }
}
