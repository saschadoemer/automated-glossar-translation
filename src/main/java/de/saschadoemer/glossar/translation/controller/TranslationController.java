package de.saschadoemer.glossar.translation.controller;

import de.saschadoemer.glossar.translation.service.GlossaryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/translation")
public class TranslationController {

    private final GlossaryService glossaryService;

    public TranslationController(GlossaryService glossaryService) {
        this.glossaryService = glossaryService;
    }

    @PostMapping("/start")
    public String startTranslation(@RequestParam String targetLanguage,
                                   @RequestParam(required = false, defaultValue = "false") boolean fuzzy,
                                   @RequestParam(required = false, defaultValue = "gemini") String llmType,
                                   @RequestParam(required = false) Integer threshold,
                                   @RequestParam(required = false, defaultValue = "3") int waitTime) {
        
        // Run in background as it might take a long time
        CompletableFuture.runAsync(() -> {
            glossaryService.processAll(targetLanguage, fuzzy, llmType, threshold, waitTime);
        });

        return "Translation process started for language: " + targetLanguage + ". Check logs for progress.";
    }
}
