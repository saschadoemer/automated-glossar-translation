package de.saschadoemer.glossar.translation.service;

import de.saschadoemer.glossar.translation.model.DictionaryEntry;
import de.saschadoemer.glossar.translation.model.TranslationJob;
import de.saschadoemer.glossar.translation.model.TranslationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Glossary service implementation.
 */
@Service
public class GlossaryService {

    private static final Logger log = LoggerFactory.getLogger(GlossaryService.class);

    private final MasterDictionaryService masterDictionaryService;
    private final ExportService exportService;
    private final Map<String, TranslationJob> jobs = new java.util.concurrent.ConcurrentHashMap<>();

    private final String openRouterApiKey;
    private final String openRouterModelName;

    public GlossaryService(MasterDictionaryService masterDictionaryService,
                           ExportService exportService,
                           @Value("${translation.openrouter.api-key:}") String openRouterApiKey,
                           @Value("${translation.openrouter.model-name:}") String openRouterModelName) {
        this.masterDictionaryService = masterDictionaryService;
        this.exportService = exportService;
        this.openRouterApiKey = openRouterApiKey;
        this.openRouterModelName = openRouterModelName;
    }

    /**
     * Verifies the availability of the configured OpenRouter model on startup.
     * This method is triggered when the application context is refreshed.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void verifyModelAvailability() {
        log.info("Checking OpenRouter model availability: model={}", openRouterModelName);
        if (openRouterApiKey == null || openRouterApiKey.isEmpty()) {
            log.warn("OpenRouter API key is not configured. Model availability check skipped.");
            return;
        }

        try (var client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create("https://openrouter.ai/api/v1/models"))
                    .header("Authorization", "Bearer " + openRouterApiKey)
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                var objectMapper = new ObjectMapper();
                var root = objectMapper.readTree(response.body());
                var data = root.path("data");
                var found = false;
                if (data.isArray()) {
                    for (JsonNode model : data) {
                        if (openRouterModelName.equals(model.path("id").asText())) {
                            found = true;
                            break;
                        }
                    }
                }

                if (found) {
                    log.info("OpenRouter model '{}' is available and verified.", openRouterModelName);
                } else {
                    log.warn("OpenRouter model '{}' was not found in the list of available models. Please check the model name.", openRouterModelName);
                }
            } else if (response.statusCode() == 401) {
                log.error("OpenRouter API key is invalid or unauthorized (401).");
            } else {
                log.error("Failed to verify model availability. OpenRouter API returned status: {}", response.statusCode());
            }
        } catch (Exception e) {
            log.error("An error occurred while verifying OpenRouter model availability", e);
        }
    }

    /**
     * Processes all records from the provided input stream.
     *
     * @param jobId          The job identifier.
     * @param inputStream    The input stream containing terms.
     * @param targetLanguage The target language for translation.
     * @param fuzzy          Whether to use fuzzy matching.
     * @param threshold      Maximum number of records to process.
     * @param waitTime       Wait time in seconds between records.
     */
    public void processAll(String jobId, java.io.InputStream inputStream, String targetLanguage, boolean fuzzy, Integer threshold, int waitTime) {
        var job = new TranslationJob(jobId, targetLanguage);
        jobs.put(jobId, job);

        log.info("Starting glossary data processing: jobId={}, targetLanguage={}, fuzzy={}, model={}, threshold={}, waitTime={}s",
                jobId, targetLanguage, fuzzy, openRouterModelName, threshold != null ? threshold : "none", waitTime);

        LlmService llmService;
        try {
            if (openRouterApiKey == null || openRouterApiKey.isEmpty()) {
                throw new IllegalStateException("OpenRouter API key not configured. Please set translation.openrouter.api-key");
            }
            llmService = new LlmService(openRouterApiKey, openRouterModelName);
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

<<<<<<< HEAD
                try {
                    var result = process(csvRecord, targetLanguage, dictionary, fuzzy, llmService);
                    if (result != null) {
                        results.add(result);
                        // Update partial results
                        job.setResultData(exportService.exportToExcel(results));
                    }
                } catch (Exception e) {
                    log.error("Unexpected error processing record for jobId={}: {}", jobId, csvRecord, e);
                    var errorResult = new TranslationResult();
                    if (csvRecord.size() > 0) {
                        errorResult.setContent(csvRecord.get(0));
                    }
                    errorResult.setComments("Unexpected error during processing: " + e.getMessage());
                    results.add(errorResult);
=======
                var result = process(csvRecord, targetLanguage, dictionary, fuzzy, llmService);
                if (result != null) {
                    results.add(result);
                    // Update partial results
>>>>>>> origin/main
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
        var totalInputTokens = 0;
        var totalOutputTokens = 0;
        var totalTokens = 0;
        var totalDuration = 0L;
        for (var res : results) {
            if (res.getInputTokens() != null) totalInputTokens += res.getInputTokens();
            if (res.getOutputTokens() != null) totalOutputTokens += res.getOutputTokens();
            if (res.getTotalTokens() != null) totalTokens += res.getTotalTokens();
            if (res.getDurationMs() != null) totalDuration += res.getDurationMs();
        }
        log.info("Summary for jobId={}:", jobId);
        log.info("- Total entries processed: {}", results.size());
        log.info("- Total tokens: {} (input: {}, output: {})", totalTokens, totalInputTokens, totalOutputTokens);
        log.info("- Total duration (LLM calls): {}ms ({}s)", totalDuration, totalDuration / 1000.0);
    }

    public TranslationJob getJobStatus(String jobId) {
        return jobs.get(jobId);
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
