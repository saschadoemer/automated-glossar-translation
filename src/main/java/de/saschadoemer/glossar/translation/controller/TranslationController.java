package de.saschadoemer.glossar.translation.controller;

import de.saschadoemer.glossar.translation.service.GlossaryService;
import de.saschadoemer.glossar.translation.service.MasterDictionaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
     * @return A response entity indicating the process status.
     */
    @PostMapping(value = "/start", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Start translation process",
            description = "Uploads a CSV file and starts the translation process asynchronously.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Translation process started successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid input file"),
                    @ApiResponse(responseCode = "412", description = "Master dictionary not set"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<String> startTranslation(
            @Parameter(description = "CSV file with terms", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "Target language code (e.g., en-US)", required = true) @RequestParam String targetLanguage,
            @Parameter(description = "Enable fuzzy matching") @RequestParam(required = false, defaultValue = "false") boolean fuzzy,
            @Parameter(description = "LLM provider type") @RequestParam(required = false, defaultValue = "gemini") String llmType,
            @Parameter(description = "Limit number of processed entries") @RequestParam(required = false) Integer threshold,
            @Parameter(description = "Wait time between requests in seconds") @RequestParam(required = false, defaultValue = "3") int waitTime) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: Input file is empty. Please provide a CSV file with terms.");
        }

        if (!masterDictionaryService.isMasterDictionarySet()) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                    .body("Error: Master dictionary has not been set. Please upload it first.");
        }

        try {
            var fileBytes = file.getBytes();

            CompletableFuture.runAsync(() -> {
                try (var bais = new ByteArrayInputStream(fileBytes)) {
                    glossaryService.processAll(bais, targetLanguage, fuzzy, llmType, threshold, waitTime);
                } catch (IOException e) {
                    throw new RuntimeException("Error processing translation input", e);
                }
            });

            return ResponseEntity.ok("Translation process started for language: " + targetLanguage + ". Check logs for progress.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading input file: " + e.getMessage());
        }
    }

    /**
     * Uploads and processes a master dictionary Excel file.
     *
     * @param file The Excel file containing the master dictionary.
     * @return A response entity indicating the upload status.
     */
    @PostMapping(value = "/master-dictionary/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload master dictionary",
            description = "Uploads an Excel file to be used as the master dictionary for translations.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Master dictionary uploaded successfully"),
                    @ApiResponse(responseCode = "400", description = "No file selected"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<String> uploadMasterDictionary(
            @Parameter(description = "Excel file with dictionary data", required = true) @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload.");
        }

        try {
            masterDictionaryService.setMasterDictionary(file.getInputStream());
            return ResponseEntity.ok("Master dictionary uploaded and processed successfully.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing master dictionary: " + e.getMessage());
        }
    }
}
