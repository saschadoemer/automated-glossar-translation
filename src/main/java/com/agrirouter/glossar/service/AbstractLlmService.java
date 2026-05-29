package com.agrirouter.glossar.service;

import com.agrirouter.glossar.model.DictionaryEntry;
import com.agrirouter.glossar.model.TranslationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
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
        PromptTemplate template = PromptTemplate.from(promptTemplateText);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("content", content);
        variables.put("targetLanguage", targetLanguage);
        
        String contextText = context == null || context.isEmpty() 
            ? "No context available." 
            : context.stream()
                .map(DictionaryEntry::toString)
                .collect(Collectors.joining("\n"));
        variables.put("context", contextText);

        Prompt prompt = template.apply(variables);
        String response = model.chat(prompt.text());

        try {
            String json = response.trim();
            if (json.startsWith("```json")) {
                json = json.substring(7);
            }
            if (json.startsWith("```")) {
                json = json.substring(3);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            TranslationResult result = objectMapper.readValue(json.trim(), TranslationResult.class);

            // Post-processing to ensure requirements are met
            if (context == null || context.isEmpty()) {
                result.setTranslationWithContext(null);
                result.setConfidence(null);
                String noContextComment = "No matches within the context.";
                if (result.getComments() == null || result.getComments().isEmpty()) {
                    result.setComments(noContextComment);
                } else if (!result.getComments().contains(noContextComment)) {
                    result.setComments(noContextComment + " " + result.getComments());
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("Error parsing LLM response: {}", response, e);
            TranslationResult errorResult = new TranslationResult();
            errorResult.setContent(content);
            errorResult.setComments("Error parsing LLM response. Original response: " + response);
            return errorResult;
        }
    }
}
