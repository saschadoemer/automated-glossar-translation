package de.knipex.glossar.service;

import de.knipex.glossar.model.DictionaryEntry;
import de.knipex.glossar.model.TranslationResult;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Glossary service implementation.
 */
public class GlossaryServiceImpl implements GlossaryService {

    private static final Logger logger = LoggerFactory.getLogger(GlossaryServiceImpl.class);
    private final Map<String, List<DictionaryEntry>> dictionary;
    private final String targetLanguage;
    private final boolean fuzzy;
    private final LlmService llmService;

    public GlossaryServiceImpl(String targetLanguage, Map<String, List<DictionaryEntry>> dictionary, boolean fuzzy, LlmService llmService) {
        this.dictionary = dictionary;
        this.targetLanguage = targetLanguage;
        this.fuzzy = fuzzy;
        this.llmService = llmService;
        logger.info("Glossary service initialized for language: {} (fuzzy: {}, llm: {})", targetLanguage, fuzzy, llmService.getClass().getSimpleName());
    }

    @Override
    public TranslationResult process(CSVRecord record) {
        if (record.size() > 0) {
            String entry = record.get(0);
            logger.debug("[{}] Processing entry: {}", targetLanguage, entry);

            List<DictionaryEntry> matchingEntries;
            if (fuzzy) {
                matchingEntries = findFuzzyMatches(entry);
            } else {
                matchingEntries = dictionary.get(entry);
            }

            if (matchingEntries != null && !matchingEntries.isEmpty()) {
                logger.info("[{}] Gathered context for '{}': {} matches found.", targetLanguage, entry, matchingEntries.size());
            } else {
                logger.warn("[{}] No matching entries found for: {}.", targetLanguage, entry);
            }

            TranslationResult result = llmService.translate(entry, matchingEntries, targetLanguage);
            System.out.println("--------------------------------------------------");
            System.out.print(result.toString());
            System.out.println("--------------------------------------------------");
            return result;
        }
        return null;
    }

    private List<DictionaryEntry> findFuzzyMatches(String entry) {
        String searchEntry = entry.toLowerCase();
        if (searchEntry.endsWith(".")) {
            searchEntry = searchEntry.substring(0, searchEntry.length() - 1);
        }

        if (searchEntry.isEmpty()) {
            return List.of();
        }

        final String finalSearchEntry = searchEntry;
        return dictionary.entrySet().stream()
                .filter(e -> {
                    String key = e.getKey().toLowerCase();
                    return key.contains(finalSearchEntry) || finalSearchEntry.contains(key);
                })
                .flatMap(e -> e.getValue().stream())
                .distinct()
                .toList();
    }
}
