package com.agrirouter.glossar;

import com.agrirouter.glossar.service.GlossaryService;
import com.agrirouter.glossar.service.SpanishGlossaryService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String GLOSSAR_DATA_FILE = "/glossar-master-data.csv";

    public static void main(String[] args) {
        if (args.length < 1) {
            logger.error("Usage: java -jar ... <target-language-code>");
            System.exit(1);
        }
        String targetLanguage = args[0];
        new Main().run(targetLanguage);
    }

    public void run(String targetLanguage) {
        logger.info("Starting glossary data processing for language: {}", targetLanguage);

        GlossaryService glossaryService;
        if ("es-ES".equalsIgnoreCase(targetLanguage)) {
            glossaryService = new SpanishGlossaryService();
        } else {
            logger.error("Unsupported language: {}", targetLanguage);
            return;
        }

        try (var inputStream = getClass().getResourceAsStream(GLOSSAR_DATA_FILE)) {
            if (inputStream == null) {
                logger.error("Resource not found: {}", GLOSSAR_DATA_FILE);
                return;
            }

            try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 var csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {
                for (var csvRecord : csvParser) {
                    glossaryService.process(csvRecord);
                }
            }
        } catch (Exception e) {
            logger.error("Error during glossary processing", e);
        }

        logger.info("Glossary data processing finished.");
    }
}
