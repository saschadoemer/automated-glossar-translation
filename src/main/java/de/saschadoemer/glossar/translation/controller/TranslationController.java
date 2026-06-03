package de.saschadoemer.glossar.translation.controller;

import de.saschadoemer.glossar.translation.service.GlossaryService;
import de.saschadoemer.glossar.translation.service.MasterDictionaryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/translation")
public class TranslationController {

    private final GlossaryService glossaryService;
    private final MasterDictionaryService masterDictionaryService;

    public TranslationController(GlossaryService glossaryService, MasterDictionaryService masterDictionaryService) {
        this.glossaryService = glossaryService;
        this.masterDictionaryService = masterDictionaryService;
    }

    @PostMapping("/start")
    public ResponseEntity<String> startTranslation(@RequestParam("file") MultipartFile file,
                                                   @RequestParam String targetLanguage,
                                                   @RequestParam(required = false, defaultValue = "false") boolean fuzzy,
                                                   @RequestParam(required = false, defaultValue = "gemini") String llmType,
                                                   @RequestParam(required = false) Integer threshold,
                                                   @RequestParam(required = false, defaultValue = "3") int waitTime) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: Input file is empty. Please provide a CSV file with terms.");
        }

        if (!masterDictionaryService.isMasterDictionarySet()) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                    .body("Error: Master dictionary has not been set. Please upload it first.");
        }

        try {
            // Read bytes to allow async processing or handle carefully
            byte[] fileBytes = file.getBytes();
            
            // Run in background as it might take a long time
            CompletableFuture.runAsync(() -> {
                try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(fileBytes)) {
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

    @PostMapping("/master-dictionary/upload")
    public ResponseEntity<String> uploadMasterDictionary(@RequestParam("file") MultipartFile file) {
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
