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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Glossary service implementation.
 */
@Service
public class GlossaryServiceImpl implements GlossaryService {

    private static final Logger logger = LoggerFactory.getLogger(GlossaryServiceImpl.class);
    private static final String GLOSSAR_DATA_FILE = "/glossar-master-data.csv";

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

    public void processAll(String targetLanguage, boolean fuzzy, String llmType, Integer threshold, int waitTime) {
        String outputFile = "glossary-translation-results-" + targetLanguage.toLowerCase() + ".xlsx";
        logger.info("Starting glossary data processing for language: {} (fuzzy: {}, llm: {}, threshold: {}, waitTime: {}s)",
                targetLanguage, fuzzy, llmType, threshold != null ? threshold : "none", waitTime);

        LlmService llmService;
        if ("gemini".equals(llmType)) {
            String apiKey = System.getenv("GEMINI_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                throw new RuntimeException("GEMINI_API_KEY environment variable not set.");
            }
            llmService = new GeminiLlmService(apiKey);
        } else {
            String apiKey = System.getenv("OPENAI_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                throw new RuntimeException("OPENAI_API_KEY environment variable not set.");
            }
            llmService = new OpenAiLlmService(apiKey);
        }

        Map<String, List<DictionaryEntry>> dictionary = masterDictionaryService.load(targetLanguage);

        List<TranslationResult> results = exportService.loadFromExcel(outputFile);
        String lastProcessedId = stateService.loadLastProcessedId(targetLanguage);
        boolean skipping = lastProcessedId != null;

        try (var inputStream = getClass().getResourceAsStream(GLOSSAR_DATA_FILE)) {
            if (inputStream == null) {
                logger.error("Resource not found: {}", GLOSSAR_DATA_FILE);
                return;
            }

            try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 var csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {
                int count = 0;
                boolean thresholdReached = false;
                for (var csvRecord : csvParser) {
                    if (csvRecord.size() == 0) continue;
                    String currentId = csvRecord.get(0);

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

                    TranslationResult result = process(csvRecord, targetLanguage, dictionary, fuzzy, llmService);
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
            double totalCost = 0;
            long totalDuration = 0;
            for (TranslationResult res : results) {
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
            String entry = record.get(0);
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

            TranslationResult result = llmService.translate(entry, matchingEntries, targetLanguage);
            System.out.println("--------------------------------------------------");
            System.out.print(result.toString());
            System.out.println("--------------------------------------------------");
            return result;
        }
        return null;
    }

    private List<DictionaryEntry> findFuzzyMatches(String entry, Map<String, List<DictionaryEntry>> dictionary) {
        String searchEntry = entry.toLowerCase();
        if (searchEntry.endsWith(".")) {
            searchEntry = searchEntry.substring(0, searchEntry.length() - 1);
        }

        if (searchEntry.isEmpty()) {
            return List.of();
        }

        final String finalSearchEntry = searchEntry;
        return dictionary.entrySet().stream()
                .filter(e -> {
                    String key = e.getKey().toLowerCase();
                    return key.contains(finalSearchEntry) || finalSearchEntry.contains(key);
                })
                .flatMap(e -> e.getValue().stream())
                .distinct()
                .toList();
    }
}
