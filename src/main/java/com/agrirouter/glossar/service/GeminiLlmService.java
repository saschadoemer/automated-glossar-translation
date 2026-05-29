package com.agrirouter.glossar.service;


import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

/**
 * Google Gemini implementation of LlmService.
 */
public class GeminiLlmService extends AbstractLlmService {

    public GeminiLlmService(String apiKey) {
        super(GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-1.5-pro") // Defaulting to a strong model
                .build());
    }
}
