package de.knipex.glossar.service;

import de.knipex.glossar.model.TranslationResult;
import org.apache.commons.csv.CSVRecord;

/**
 * Service to process glossary entries.
 */
public interface GlossaryService {
    /**
     * Process a single CSV record.
     *
     * @param record The record to process.
     * @return The translation result.
     */
    TranslationResult process(CSVRecord record);
}
