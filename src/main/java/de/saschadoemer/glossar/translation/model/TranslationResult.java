package de.saschadoemer.glossar.translation.model;

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

    /**
     * Returns the content to be translated.
     * @return the content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content to be translated.
     * @param content the content.
     */
    public void setContent(String content) {
        this.content = content;
    }
    
    /**
     * Returns the translation with context.
     * @return the translation.
     */
    public String getTranslationWithContext() {
        return translationWithContext;
    }

    /**
     * Sets the translation with context.
     * @param translationWithContext the translation.
     */
    public void setTranslationWithContext(String translationWithContext) {
        this.translationWithContext = translationWithContext;
    }

    /**
     * Returns the confidence score.
     * @return the confidence.
     */
    public Double getConfidence() {
        return confidence;
    }

    /**
     * Sets the confidence score.
     * @param confidence the confidence.
     */
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    /**
     * Returns the translation without context.
     * @return the translation.
     */
    public String getTranslationWithoutContext() {
        return translationWithoutContext;
    }

    /**
     * Sets the translation without context.
     * @param translationWithoutContext the translation.
     */
    public void setTranslationWithoutContext(String translationWithoutContext) {
        this.translationWithoutContext = translationWithoutContext;
    }

    /**
     * Returns the list of synonyms.
     * @return the synonyms.
     */
    public List<String> getSynonyms() {
        return synonyms;
    }

    /**
     * Sets the list of synonyms.
     * @param synonyms the synonyms.
     */
    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
    }

    /**
     * Returns any comments or notes.
     * @return the comments.
     */
    public String getComments() {
        return comments;
    }

    /**
     * Sets any comments or notes.
     * @param comments the comments.
     */
    public void setComments(String comments) {
        this.comments = comments;
    }

    /**
     * Returns the cost of the translation.
     * @return the cost.
     */
    public Double getCost() {
        return cost;
    }

    /**
     * Sets the cost of the translation.
     * @param cost the cost.
     */
    public void setCost(Double cost) {
        this.cost = cost;
    }

    /**
     * Returns the duration of the translation call in milliseconds.
     * @return the duration.
     */
    public Long getDurationMs() {
        return durationMs;
    }

    /**
     * Sets the duration of the translation call.
     * @param durationMs the duration.
     */
    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
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
