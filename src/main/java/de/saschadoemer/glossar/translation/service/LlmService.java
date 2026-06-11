package de.saschadoemer.glossar.translation.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import de.saschadoemer.glossar.translation.model.DictionaryEntry;
import de.saschadoemer.glossar.translation.model.TranslationResult;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to interact with LLM for translations via OpenRouter.
 */
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);
    private static final String PROMPT_PATH = "/prompts/translation_prompt.txt";
    private final ChatModel model;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String promptTemplateText;

    public LlmService(String apiKey, String modelName) {
        this.model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl("https://openrouter.ai/api/v1")
                .modelName(modelName)
                .build();
        this.promptTemplateText = loadPromptTemplate();
    }

    private String loadPromptTemplate() {
        try (var inputStream = getClass().getResourceAsStream(PROMPT_PATH)) {
            if (inputStream == null) {
                throw new RuntimeException("Prompt template not found: " + PROMPT_PATH);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error loading prompt template", e);
        }
    }

    /**
     * Translates a term based on context and target language.
     *
     * @param content        The term to translate.
     * @param context        The context gathered from the master dictionary.
     * @param targetLanguage The target language code.
     * @return The translation result.
     */
    public TranslationResult translate(String content, List<DictionaryEntry> context, String targetLanguage) {
        var template = PromptTemplate.from(promptTemplateText);

        var variables = new HashMap<String, Object>();
        variables.put("content", content);
        variables.put("targetLanguage", targetLanguage);

        var contextText = context == null || context.isEmpty()
                ? "No context available."
                : context.stream()
                .map(de -> String.format("Identifier: %s, German: %s, Translation for %s: %s",
                        de.getIdentifier(), de.getGerman(), targetLanguage, de.getTranslation(targetLanguage)))
                .collect(Collectors.joining("\n"));
        variables.put("context", contextText);

        var prompt = template.apply(variables);

        var startTime = System.currentTimeMillis();
        try {
            var chatResponse = model.chat(UserMessage.from(prompt.text()));
            var durationMs = System.currentTimeMillis() - startTime;

            var response = chatResponse.aiMessage().text();
            var usage = chatResponse.tokenUsage();

            var json = response.trim();
            // Remove markdown code blocks if present
            if (json.startsWith("```json")) {
                json = json.substring(7);
            } else if (json.startsWith("```")) {
                json = json.substring(3);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            var result = objectMapper.readValue(json.trim(), TranslationResult.class);

            if (context == null || context.isEmpty()) {
                result.setTranslationWithContext(null);
                result.setConfidence(null);
                var noContextComment = "No matches within the context.";
                if (result.getComments() == null || result.getComments().isEmpty()) {
                    result.setComments(noContextComment);
                } else if (!result.getComments().contains(noContextComment)) {
                    result.setComments(noContextComment + " " + result.getComments());
                }
            }

            result.setDurationMs(durationMs);
            if (usage != null) {
                result.setInputTokens(usage.inputTokenCount());
                result.setOutputTokens(usage.outputTokenCount());
                result.setTotalTokens(usage.totalTokenCount());
            }

            return result;
        } catch (Exception e) {
            var durationMs = System.currentTimeMillis() - startTime;
            log.error("Error during LLM translation for content='{}': {}", content, e.getMessage(), e);
            var errorResult = new TranslationResult();
            errorResult.setContent(content);
            errorResult.setComments("Error during translation: " + e.getMessage());
            errorResult.setDurationMs(durationMs);
            return errorResult;
        }
    }
}
