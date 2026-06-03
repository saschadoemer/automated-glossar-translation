package de.saschadoemer.glossar.translation.service;

import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * OpenAI implementation of LlmService.
 */
public class OpenAiLlmService extends AbstractLlmService {

    public OpenAiLlmService(String apiKey, String modelName) {
        super(OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build());
    }

    @Override
    protected double calculateCost(int inputTokens, int outputTokens) {
        // Rough pricing for gpt-4o: $0.005 / 1k input, $0.015 / 1k output
        return (inputTokens * 0.005 / 1000.0) + (outputTokens * 0.015 / 1000.0);
    }
}
