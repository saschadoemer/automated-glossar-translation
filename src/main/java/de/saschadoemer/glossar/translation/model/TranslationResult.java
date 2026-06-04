package de.saschadoemer.glossar.translation.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Result of an LLM translation.
 */
@Schema(description = "Represents the result of an LLM-based translation")
public class TranslationResult {

    @Schema(description = "The original content to be translated", example = "Identifier")
    private String content;

    @Schema(description = "The translation generated with dictionary context", example = "Bezeichner")
    private String translationWithContext;

    @Schema(description = "Confidence score of the translation (0.0 to 1.0)", example = "0.95")
    private Double confidence;

    @Schema(description = "The translation generated without dictionary context", example = "Kennung")
    private String translationWithoutContext;

    @Schema(description = "List of potential synonyms")
    private List<String> synonyms;

    @Schema(description = "Additional comments or notes from the LLM")
    private String comments;

    @Schema(description = "Rough cost of the translation in USD", example = "0.000123")
    private Double cost;

    @Schema(description = "Duration of the LLM call in milliseconds", example = "1200")
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
