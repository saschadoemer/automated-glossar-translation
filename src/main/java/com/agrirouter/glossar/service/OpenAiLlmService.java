package com.agrirouter.glossar.service;

import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * OpenAI implementation of LlmService.
 */
public class OpenAiLlmService extends AbstractLlmService {

    public OpenAiLlmService(String apiKey) {
        super(OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o") // Defaulting to a strong model
                .build());
    }
}
