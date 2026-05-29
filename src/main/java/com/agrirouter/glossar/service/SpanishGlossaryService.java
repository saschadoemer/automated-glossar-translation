package com.agrirouter.glossar.service;

import com.agrirouter.glossar.model.DictionaryEntry;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Spanish glossary service.
 */
public class SpanishGlossaryService implements GlossaryService {

    private static final Logger logger = LoggerFactory.getLogger(SpanishGlossaryService.class);
    private final Map<String, DictionaryEntry> dictionary;

    public SpanishGlossaryService(Map<String, DictionaryEntry> dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public void process(CSVRecord record) {
        if (record.size() > 0) {
            String entry = record.get(0);
            logger.info("[es-ES] Processing entry: {}", entry);
            if (dictionary.containsKey(entry)) {
                logger.info("[es-ES] Found in dictionary: {}", dictionary.get(entry));
            }
        }
    }
}
