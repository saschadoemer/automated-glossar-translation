package com.agrirouter.glossar.service;

import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * OpenAI implementation of LlmService.
 */
public class OpenAiLlmService extends AbstractLlmService {

    public OpenAiLlmService(String apiKey) {
        super(OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(getModelName())
                .build());
    }

    private static String getModelName() {
        String modelName = System.getenv("OPENAI_MODEL_NAME");
        return (modelName == null || modelName.isBlank()) ? "gpt-4o" : modelName;
    }
}
