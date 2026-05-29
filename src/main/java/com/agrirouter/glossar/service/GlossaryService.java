package com.agrirouter.glossar.service;

import org.apache.commons.csv.CSVRecord;

/**
 * Service to process glossary entries.
 */
public interface GlossaryService {
    /**
     * Process a single CSV record.
     *
     * @param record The record to process.
     */
    void process(CSVRecord record);
}
