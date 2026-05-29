package de.knipex.glossar.model;

import java.util.List;

/**
 * Result of an LLM translation.
 */
public class TranslationResult {
    private String content;
    private String translationWithContext;
    private Double confidence;
    private String translationWithoutContext;
    private List<String> synonyms;
    private String comments;
    private Double cost;
    private Long durationMs;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTranslationWithContext() {
        return translationWithContext;
    }

    public void setTranslationWithContext(String translationWithContext) {
        this.translationWithContext = translationWithContext;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getTranslationWithoutContext() {
        return translationWithoutContext;
    }

    public void setTranslationWithoutContext(String translationWithoutContext) {
        this.translationWithoutContext = translationWithoutContext;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("1) Content: ").append(content).append("\n");
        if (translationWithContext != null && !translationWithContext.isEmpty()) {
            sb.append("2) Translation (with context): ").append(translationWithContext).append("\n");
            sb.append("3) Confidence: ").append(confidence).append("\n");
        }
        sb.append("4) Translation (without context): ").append(translationWithoutContext).append("\n");
        sb.append("5) Synonyms: ").append(synonyms != null ? String.join(", ", synonyms) : "none").append("\n");
        sb.append("6) Comments: ").append(comments).append("\n");
        sb.append("7) Cost: ").append(cost != null ? String.format("%.6f", cost) : "unknown").append("\n");
        sb.append("8) Duration: ").append(durationMs != null ? durationMs + "ms" : "unknown").append("\n");
        return sb.toString();
    }
}
