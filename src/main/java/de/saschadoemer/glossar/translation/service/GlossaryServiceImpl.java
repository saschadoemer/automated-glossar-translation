package de.saschadoemer.glossar.translation.service;

import de.saschadoemer.glossar.translation.model.DictionaryEntry;
import de.saschadoemer.glossar.translation.model.TranslationResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Glossary service implementation.
 */
@Service
public class GlossaryServiceImpl implements GlossaryService {

    private static final Logger logger = LoggerFactory.getLogger(GlossaryServiceImpl.class);

    private final MasterDictionaryService masterDictionaryService;
    private final ExportService exportService;
    private final StateService stateService;

    public GlossaryServiceImpl(MasterDictionaryService masterDictionaryService,
                               ExportService exportService,
                               StateService stateService) {
        this.masterDictionaryService = masterDictionaryService;
        this.exportService = exportService;
        this.stateService = stateService;
    }

    /**
     * Processes all records from the provided input stream.
     *
     * @param inputStream    The input stream containing terms.
     * @param targetLanguage The target language for translation.
     * @param fuzzy          Whether to use fuzzy matching.
     * @param llmType        The LLM provider type.
     * @param threshold      Maximum number of records to process.
     * @param waitTime       Wait time in seconds between records.
     */
    @Override
    public void processAll(java.io.InputStream inputStream, String targetLanguage, boolean fuzzy, String llmType, Integer threshold, int waitTime) {
        var outputFile = "glossary-translation-results-" + targetLanguage.toLowerCase() + ".xlsx";
        logger.info("Starting glossary data processing for language: {} (fuzzy: {}, llm: {}, threshold: {}, waitTime: {}s)",
                targetLanguage, fuzzy, llmType, threshold != null ? threshold : "none", waitTime);

        LlmService llmService;
        if ("gemini".equals(llmType)) {
            var apiKey = System.getenv("GEMINI_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                throw new RuntimeException("GEMINI_API_KEY environment variable not set.");
            }
            llmService = new GeminiLlmService(apiKey);
        } else {
            var apiKey = System.getenv("OPENAI_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                throw new RuntimeException("OPENAI_API_KEY environment variable not set.");
            }
            llmService = new OpenAiLlmService(apiKey);
        }

        var dictionary = masterDictionaryService.load(targetLanguage);
        if (dictionary.isEmpty() && !masterDictionaryService.isMasterDictionarySet()) {
            logger.error("Master dictionary has not been set.");
            return;
        }

        var results = exportService.loadFromExcel(outputFile);
        var lastProcessedId = stateService.loadLastProcessedId(targetLanguage);
        var skipping = lastProcessedId != null;

        try {
            try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 var csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {
                var count = 0;
                var thresholdReached = false;
                for (var csvRecord : csvParser) {
                    if (csvRecord.size() == 0) continue;
                    var currentId = csvRecord.get(0);

                    if (skipping) {
                        if (currentId.equals(lastProcessedId)) {
                            skipping = false;
                        }
                        continue;
                    }

                    if (threshold != null && count >= threshold) {
                        logger.info("Threshold reached ({} entries). Stopping.", threshold);
                        thresholdReached = true;
                        break;
                    }

                    if (count > 0 && waitTime > 0) {
                        try {
                            logger.debug("Waiting for {} seconds before next entry...", waitTime);
                            Thread.sleep(waitTime * 1000L);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            logger.warn("Wait time interrupted", e);
                        }
                    }

                    var result = process(csvRecord, targetLanguage, dictionary, fuzzy, llmService);
                    if (result != null) {
                        results.add(result);
                        stateService.saveLastProcessedId(currentId, targetLanguage);
                        exportService.exportToExcel(results, outputFile);
                    }
                    count++;
                }

                if (!thresholdReached && !skipping) {
                    stateService.clear(targetLanguage);
                } else if (skipping) {
                    logger.warn("Last processed ID '{}' not found in CSV. Resume failed.", lastProcessedId);
                }
            }
        } catch (Exception e) {
            logger.error("Error during glossary processing", e);
        }

        if (!results.isEmpty()) {
            var totalCost = 0.0;
            var totalDuration = 0L;
            for (var res : results) {
                if (res.getCost() != null) totalCost += res.getCost();
                if (res.getDurationMs() != null) totalDuration += res.getDurationMs();
            }
            logger.info("Summary for the whole process:");
            logger.info("- Total entries processed: {}", results.size());
            logger.info("- Total cost (rough): ${}", String.format("%.6f", totalCost));
            logger.info("- Total duration (LLM calls): {}ms ({}s)", totalDuration, totalDuration / 1000.0);

            exportService.exportToExcel(results, outputFile);
        } else {
            logger.warn("No results to export.");
        }

        logger.info("Glossary data processing finished.");
    }

    @Override
    public TranslationResult process(CSVRecord record) {
        throw new UnsupportedOperationException("Use process with context instead or the processAll method.");
    }

    private TranslationResult process(CSVRecord record, String targetLanguage, Map<String, List<DictionaryEntry>> dictionary, boolean fuzzy, LlmService llmService) {
        if (record.size() > 0) {
            var entry = record.get(0);
            logger.debug("[{}] Processing entry: {}", targetLanguage, entry);

            List<DictionaryEntry> matchingEntries;
            if (fuzzy) {
                matchingEntries = findFuzzyMatches(entry, dictionary);
            } else {
                matchingEntries = dictionary.get(entry);
            }

            if (matchingEntries != null && !matchingEntries.isEmpty()) {
                logger.info("[{}] Gathered context for '{}': {} matches found.", targetLanguage, entry, matchingEntries.size());
            } else {
                logger.warn("[{}] No matching entries found for: {}.", targetLanguage, entry);
            }

            return llmService.translate(entry, matchingEntries, targetLanguage);
        }
        return null;
    }

    private List<DictionaryEntry> findFuzzyMatches(String entry, Map<String, List<DictionaryEntry>> dictionary) {
        var searchEntry = entry.toLowerCase();
        if (searchEntry.endsWith(".")) {
            searchEntry = searchEntry.substring(0, searchEntry.length() - 1);
        }

        if (searchEntry.isEmpty()) {
            return List.of();
        }

        final var finalSearchEntry = searchEntry;
        return dictionary.entrySet().stream()
                .filter(e -> {
                    var key = e.getKey().toLowerCase();
                    return key.contains(finalSearchEntry) || finalSearchEntry.contains(key);
                })
                .flatMap(e -> e.getValue().stream())
                .distinct()
                .toList();
    }
}
