package de.knipex.glossar.service;


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

    @Override
    protected double calculateCost(int inputTokens, int outputTokens) {
        // Rough pricing for gemini-1.5-pro: $0.0035 / 1k input, $0.0105 / 1k output
        return (inputTokens * 0.0035 / 1000.0) + (outputTokens * 0.0105 / 1000.0);
    }
}
