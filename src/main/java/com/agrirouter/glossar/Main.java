package com.agrirouter.glossar;

import com.agrirouter.glossar.model.DictionaryEntry;
import com.agrirouter.glossar.service.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String GLOSSAR_DATA_FILE = "/glossar-master-data.csv";

    public static void main(String[] args) {
        if (args.length < 1) {
            logger.error("Usage: java -jar ... <target-language-code> [--fuzzy] [--llm <openai|gemini>] [--threshold <number>]");
            System.exit(1);
        }
        String targetLanguage = args[0];
        boolean fuzzy = false;
        String llmType = "gemini";
        int threshold = 5; // Default threshold for testing
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
        }
        new Main().run(targetLanguage, fuzzy, llmType, threshold);
    }

    public void run(String targetLanguage, boolean fuzzy, String llmType, Integer threshold) {
        logger.info("Starting glossary data processing for language: {} (fuzzy: {}, llm: {}, threshold: {})",
                targetLanguage, fuzzy, llmType, threshold != null ? threshold : "none");

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

        try (var inputStream = getClass().getResourceAsStream(GLOSSAR_DATA_FILE)) {
            if (inputStream == null) {
                logger.error("Resource not found: {}", GLOSSAR_DATA_FILE);
                return;
            }

            try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 var csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {
                int count = 0;
                for (var csvRecord : csvParser) {
                    if (threshold != null && count >= threshold) {
                        logger.info("Threshold reached ({} entries). Stopping.", threshold);
                        break;
                    }
                    glossaryService.process(csvRecord);
                    count++;
                }
            }
        } catch (Exception e) {
            logger.error("Error during glossary processing", e);
        }

        logger.info("Glossary data processing finished.");
    }
}
