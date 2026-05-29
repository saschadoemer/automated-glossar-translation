package com.agrirouter.glossar.service;

import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spanish glossary service.
 */
public class SpanishGlossaryService implements GlossaryService {

    private static final Logger logger = LoggerFactory.getLogger(SpanishGlossaryService.class);

    @Override
    public void process(CSVRecord record) {
        if (record.size() > 0) {
            String entry = record.get(0);
            logger.info("[es-ES] Processing entry: {}", entry);
        }
    }
}
