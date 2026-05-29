package com.agrirouter.glossar.service;

import com.agrirouter.glossar.model.DictionaryEntry;
import com.agrirouter.glossar.model.GlossaryContext;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Spanish glossary service.
 */
public class GlossaryServiceImpl implements GlossaryService {

    private static final Logger logger = LoggerFactory.getLogger(GlossaryServiceImpl.class);
    private final Map<String, List<DictionaryEntry>> dictionary;
    private final String targetLanguage;

    public GlossaryServiceImpl(String targetLanguage, Map<String, List<DictionaryEntry>> dictionary) {
        this.dictionary = dictionary;
        this.targetLanguage = targetLanguage;
        logger.info("Glossary service initialized for language: {}", targetLanguage);
    }

    @Override
    public void process(CSVRecord record) {
        if (record.size() > 0) {
            String entry = record.get(0);
            logger.debug("[{}] Processing entry: {}", targetLanguage, entry);

            List<DictionaryEntry> matchingEntries = dictionary.get(entry);
            if (matchingEntries != null && !matchingEntries.isEmpty()) {
                GlossaryContext context = new GlossaryContext(entry, matchingEntries);
                logger.info("[{}] Gathered context for '{}': {} matches found.", targetLanguage, entry, matchingEntries.size());
                logger.debug("[{}] The context gathered is the following: {}", targetLanguage, context);
            } else {
                logger.warn("[{}] No matching entries found for: {}. Skipping.", targetLanguage, entry);
            }
        }
    }
}
