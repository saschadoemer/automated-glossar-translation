package com.agrirouter.glossar.service;


import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

/**
 * Google Gemini implementation of LlmService.
 */
public class GeminiLlmService extends AbstractLlmService {

    public GeminiLlmService(String apiKey) {
        super(GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(getModelName())
                .build());
    }

    private static String getModelName() {
        String modelName = System.getenv("GEMINI_MODEL_NAME");
        return (modelName == null || modelName.isBlank()) ? "gemini-1.5-pro" : modelName;
    }
}
