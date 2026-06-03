package de.saschadoemer.glossar.translation.service;

import de.saschadoemer.glossar.translation.model.DictionaryEntry;
import de.saschadoemer.glossar.translation.model.TranslationJob;
import de.saschadoemer.glossar.translation.model.TranslationResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private static final Logger log = LoggerFactory.getLogger(GlossaryServiceImpl.class);

    private final MasterDictionaryService masterDictionaryService;
    private final ExportService exportService;
    private final Map<String, TranslationJob> jobs = new java.util.concurrent.ConcurrentHashMap<>();

    private final String geminiApiKey;
    private final String geminiModelName;
    private final String openAiApiKey;
    private final String openAiModelName;

    public GlossaryServiceImpl(MasterDictionaryService masterDictionaryService,
                               ExportService exportService,
                               @Value("${translation.gemini.api-key:}") String geminiApiKey,
                               @Value("${translation.gemini.model-name:gemini-1.5-pro}") String geminiModelName,
                               @Value("${translation.openai.api-key:}") String openAiApiKey,
                               @Value("${translation.openai.model-name:gpt-4o}") String openAiModelName) {
        this.masterDictionaryService = masterDictionaryService;
        this.exportService = exportService;
        this.geminiApiKey = geminiApiKey;
        this.geminiModelName = geminiModelName;
        this.openAiApiKey = openAiApiKey;
        this.openAiModelName = openAiModelName;
    }

    /**
     * Processes all records from the provided input stream.
     *
     * @param jobId          The job identifier.
     * @param inputStream    The input stream containing terms.
     * @param targetLanguage The target language for translation.
     * @param fuzzy          Whether to use fuzzy matching.
     * @param llmType        The LLM provider type.
     * @param threshold      Maximum number of records to process.
     * @param waitTime       Wait time in seconds between records.
     */
    @Override
    public void processAll(String jobId, java.io.InputStream inputStream, String targetLanguage, boolean fuzzy, String llmType, Integer threshold, int waitTime) {
        var job = new TranslationJob(jobId, targetLanguage);
        jobs.put(jobId, job);

        log.info("Starting glossary data processing: jobId={}, targetLanguage={}, fuzzy={}, llmType={}, threshold={}, waitTime={}s",
                jobId, targetLanguage, fuzzy, llmType, threshold != null ? threshold : "none", waitTime);

        LlmService llmService;
        try {
            if ("gemini".equals(llmType)) {
                if (geminiApiKey == null || geminiApiKey.isEmpty()) {
                    throw new IllegalStateException("Gemini API key not configured. Please set translation.gemini.api-key");
                }
                llmService = new GeminiLlmService(geminiApiKey, geminiModelName);
            } else {
                if (openAiApiKey == null || openAiApiKey.isEmpty()) {
                    throw new IllegalStateException("OpenAI API key not configured. Please set translation.openai.api-key");
                }
                llmService = new OpenAiLlmService(openAiApiKey, openAiModelName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize LLM service for jobId={}", jobId, e);
            job.setError("LLM initialization failed: " + e.getMessage());
            job.setCompleted(true);
            return;
        }

        var dictionary = masterDictionaryService.load(targetLanguage);
        if (dictionary.isEmpty() && !masterDictionaryService.isMasterDictionarySet()) {
            log.warn("Master dictionary is not set for jobId={}", jobId);
            job.setError("Master dictionary is not set.");
            job.setCompleted(true);
            return;
        }

        var results = new java.util.ArrayList<TranslationResult>();

        try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             var csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {
            var records = csvParser.getRecords();
            var totalToProcess = threshold != null ? Math.min(threshold, records.size()) : records.size();
            job.setTotalItems(totalToProcess);

            log.info("Processing {} records for jobId={}", totalToProcess, jobId);

            var count = 0;
            for (var csvRecord : records) {
                if (csvRecord.size() == 0) continue;

                if (threshold != null && count >= threshold) {
                    log.info("Threshold reached ({} entries) for jobId={}. Stopping.", threshold, jobId);
                    break;
                }

                if (count > 0 && waitTime > 0) {
                    try {
                        log.debug("Waiting for {} seconds before next entry for jobId={}...", waitTime, jobId);
                        Thread.sleep(waitTime * 1000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Wait time interrupted for jobId={}", jobId, e);
                    }
                }

                var result = process(csvRecord, targetLanguage, dictionary, fuzzy, llmService);
                if (result != null) {
                    results.add(result);
                    // Update partial results
                    job.setResultData(exportService.exportToExcel(results));
                }
                count++;
                job.setProcessedItems(count);
            }
        } catch (Exception e) {
            log.error("Error during glossary processing for jobId={}", jobId, e);
            job.setError("Processing failed: " + e.getMessage());
        }

        job.setCompleted(true);

        if (!results.isEmpty()) {
            logProcessSummary(jobId, results);
            job.setResultData(exportService.exportToExcel(results));
        } else {
            log.warn("No results to export for jobId={}", jobId);
        }

        log.info("Glossary data processing finished for jobId={}", jobId);
    }

    private void logProcessSummary(String jobId, List<TranslationResult> results) {
        var totalCost = 0.0;
        var totalDuration = 0L;
        for (var res : results) {
            if (res.getCost() != null) totalCost += res.getCost();
            if (res.getDurationMs() != null) totalDuration += res.getDurationMs();
        }
        log.info("Summary for jobId={}:", jobId);
        log.info("- Total entries processed: {}", results.size());
        log.info("- Total cost (rough): ${}", String.format("%.6f", totalCost));
        log.info("- Total duration (LLM calls): {}ms ({}s)", totalDuration, totalDuration / 1000.0);
    }

    @Override
    public TranslationJob getJobStatus(String jobId) {
        return jobs.get(jobId);
    }

    @Override
    public TranslationResult process(CSVRecord record) {
        throw new UnsupportedOperationException("Use process with context instead or the processAll method.");
    }

    private TranslationResult process(CSVRecord record, String targetLanguage, Map<String, List<DictionaryEntry>> dictionary, boolean fuzzy, LlmService llmService) {
        if (record.size() > 0) {
            var entry = record.get(0);
            log.debug("[{}] Processing entry: {}", targetLanguage, entry);

            List<DictionaryEntry> matchingEntries;
            if (fuzzy) {
                matchingEntries = findFuzzyMatches(entry, dictionary);
            } else {
                matchingEntries = dictionary.get(entry);
            }

            if (matchingEntries != null && !matchingEntries.isEmpty()) {
                log.info("[{}] Gathered context for '{}': {} matches found.", targetLanguage, entry, matchingEntries.size());
            } else {
                log.warn("[{}] No matching entries found for: {}.", targetLanguage, entry);
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
