package de.knipex.glossar.service;

import de.knipex.glossar.model.DictionaryEntry;
import de.knipex.glossar.model.TranslationResult;
import java.util.List;

/**
 * Service to interact with LLM for translations.
 */
public interface LlmService {
    /**
     * Translates a term based on context and target language.
     *
     * @param content The term to translate.
     * @param context The context gathered from the master dictionary.
     * @param targetLanguage The target language code.
     * @return The translation result.
     */
    TranslationResult translate(String content, List<DictionaryEntry> context, String targetLanguage);
}
