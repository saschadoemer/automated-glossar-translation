package de.saschadoemer.glossar.translation.service;

import de.saschadoemer.glossar.translation.model.DictionaryEntry;
import de.saschadoemer.glossar.translation.model.TranslationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract base class for LLM services.
 */
public abstract class AbstractLlmService implements LlmService {

    private static final Logger logger = LoggerFactory.getLogger(AbstractLlmService.class);
    private static final String PROMPT_PATH = "/prompts/translation_prompt.txt";
    private final ChatModel model;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String promptTemplateText;

    protected AbstractLlmService(ChatModel model) {
        this.model = model;
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

    @Override
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
        var chatResponse = model.chat(UserMessage.from(prompt.text()));
        var durationMs = System.currentTimeMillis() - startTime;

        var response = chatResponse.aiMessage().text();
        var usage = chatResponse.tokenUsage();

        try {
            var json = response.trim();
            if (json.startsWith("```json")) {
                json = json.substring(7);
            }
            if (json.startsWith("```")) {
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
                result.setCost(calculateCost(usage.inputTokenCount(), usage.outputTokenCount()));
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Error parsing LLM response: {}", response, e);
            var errorResult = new TranslationResult();
            errorResult.setContent(content);
            errorResult.setComments("Error parsing LLM response. Original response: " + response);
            errorResult.setDurationMs(durationMs);
            if (usage != null) {
                errorResult.setCost(calculateCost(usage.inputTokenCount(), usage.outputTokenCount()));
            }
            return errorResult;
        }
    }

    /**
     * Calculates the cost based on token usage.
     *
     * @param inputTokens  Number of input tokens.
     * @param outputTokens Number of output tokens.
     * @return The calculated cost.
     */
    protected abstract double calculateCost(int inputTokens, int outputTokens);
}
