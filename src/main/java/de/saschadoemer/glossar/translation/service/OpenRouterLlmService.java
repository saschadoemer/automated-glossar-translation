package de.saschadoemer.glossar.translation.service;

import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * OpenRouter implementation of LlmService.
 * Uses the OpenAI-compatible API provided by OpenRouter.
 */
public class OpenRouterLlmService extends AbstractLlmService {

    public OpenRouterLlmService(String apiKey, String modelName) {
        super(OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl("https://openrouter.ai/api/v1")
                .modelName(modelName)
                .build());
    }

}
