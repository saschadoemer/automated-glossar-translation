package de.knipex.glossar.service;

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

    @Override
    protected double calculateCost(int inputTokens, int outputTokens) {
        // Rough pricing for gpt-4o: $0.005 / 1k input, $0.015 / 1k output
        return (inputTokens * 0.005 / 1000.0) + (outputTokens * 0.015 / 1000.0);
    }
}
