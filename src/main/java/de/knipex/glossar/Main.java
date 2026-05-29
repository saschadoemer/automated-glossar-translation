package de.knipex.glossar;

import de.knipex.glossar.model.DictionaryEntry;
import de.knipex.glossar.model.TranslationResult;
import de.knipex.glossar.service.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String GLOSSAR_DATA_FILE = "/glossar-master-data.csv";
    private static final String OUTPUT_FILE = "glossary-translation-results.xlsx";

    public static void main(String[] args) {
        if (args.length < 1) {
            logger.error("Usage: java -jar ... <target-language-code> [--fuzzy] [--llm <openai|gemini>] [--threshold <number>] [--waitTime <seconds>]");
            System.exit(1);
        }
        String targetLanguage = args[0];
        boolean fuzzy = false;
        String llmType = "gemini";
        Integer threshold = null; // No default threshold, process whole file if not provided
        int waitTime = 3; // Default wait time in seconds
        for (int i = 0; i < args.length; i++) {
            if ("--fuzzy".equalsIgnoreCase(args[i])) {
                fuzzy = true;
            }
            if ("--llm".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                llmType = args[i + 1].toLowerCase();
            }
            if ("--threshold".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                try {
                    threshold = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException e) {
                    logger.error("Invalid threshold value: {}", args[i + 1]);
                    System.exit(1);
                }
            }
            if ("--waitTime".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                try {
                    waitTime = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException e) {
                    logger.error("Invalid waitTime value: {}", args[i + 1]);
                    System.exit(1);
                }
            }
        }
        new Main().run(targetLanguage, fuzzy, llmType, threshold, waitTime);
    }

    public void run(String targetLanguage, boolean fuzzy, String llmType, Integer threshold, int waitTime) {
        logger.info("Starting glossary data processing for language: {} (fuzzy: {}, llm: {}, threshold: {}, waitTime: {}s)",
                targetLanguage, fuzzy, llmType, threshold != null ? threshold : "none", waitTime);

        LlmService llmService;
        if ("gemini".equals(llmType)) {
            String apiKey = System.getenv("GEMINI_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                logger.error("GEMINI_API_KEY environment variable not set.");
                System.exit(1);
                return;
            }
            llmService = new GeminiLlmService(apiKey);
        } else {
            String apiKey = System.getenv("OPENAI_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                logger.error("OPENAI_API_KEY environment variable not set.");
                System.exit(1);
                return;
            }
            llmService = new OpenAiLlmService(apiKey);
        }

        MasterDictionaryService masterDictionaryService = new MasterDictionaryService();
        Map<String, List<DictionaryEntry>> dictionary = masterDictionaryService.load(targetLanguage);

        GlossaryService glossaryService = new GlossaryServiceImpl(targetLanguage, dictionary, fuzzy, llmService);
        ExportService exportService = new ExportService();
        StateService stateService = new StateService();

        List<TranslationResult> results = exportService.loadFromExcel(OUTPUT_FILE);
        String lastProcessedId = stateService.loadLastProcessedId();
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

                    TranslationResult result = glossaryService.process(csvRecord);
                    if (result != null) {
                        results.add(result);
                        stateService.saveLastProcessedId(currentId);
                        exportService.exportToExcel(results, OUTPUT_FILE);
                    }
                    count++;
                }

                if (!thresholdReached && !skipping) {
                    stateService.clear();
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

            exportService.exportToExcel(results, OUTPUT_FILE);
        } else {
            logger.warn("No results to export.");
        }

        logger.info("Glossary data processing finished.");
    }
}
