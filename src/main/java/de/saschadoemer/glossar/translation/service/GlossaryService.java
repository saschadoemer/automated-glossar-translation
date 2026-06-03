package de.saschadoemer.glossar.translation.service;

import de.saschadoemer.glossar.translation.model.TranslationResult;
import org.apache.commons.csv.CSVRecord;

/**
 * Service to process glossary entries.
 */
public interface GlossaryService {
    /**
     * Process all records for a target language.
     *
     * @param targetLanguage The target language code.
     * @param fuzzy          Whether to use fuzzy matching.
     * @param llmType        The LLM type (openai or gemini).
     * @param threshold      The maximum number of entries to process.
     * @param waitTime       Wait time between entries in seconds.
     */
    void processAll(String targetLanguage, boolean fuzzy, String llmType, Integer threshold, int waitTime);

    /**
     * Process a single CSV record.
     *
     * @param record The record to process.
     * @return The translation result.
     */
    TranslationResult process(CSVRecord record);
}
