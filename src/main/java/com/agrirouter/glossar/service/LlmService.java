package com.agrirouter.glossar.service;

import com.agrirouter.glossar.model.DictionaryEntry;
import com.agrirouter.glossar.model.TranslationResult;
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
