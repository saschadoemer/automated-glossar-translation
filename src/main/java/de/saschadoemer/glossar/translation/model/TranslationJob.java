package de.saschadoemer.glossar.translation.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Model representing a translation job.
 */
@Schema(description = "Represents a translation job and its current status")
@Entity
@Table(name = "translation_jobs")
public class TranslationJob {

    @Schema(description = "Unique job identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    @Id
    private String id;

    @Schema(description = "Target language code for the translation", example = "en-US")
    private String targetLanguage;

    @Schema(description = "Total number of items to process", example = "100")
    private int totalItems;

    @Schema(description = "Number of items already processed", example = "45")
    private int processedItems;

    @Schema(hidden = true)
    @JsonIgnore
    @Lob
    @Column(columnDefinition = "BLOB")
    private byte[] resultData;

    @Schema(description = "Whether the job has completed", example = "false")
    private boolean completed;

    @Schema(description = "Error message if the job failed", example = "Invalid API key")
    @Column(columnDefinition = "TEXT")
    private String error;

    public TranslationJob() {
        this.id = null;
        this.targetLanguage = null;
    }

    public TranslationJob(String id, String targetLanguage) {
        this.id = id;
        this.targetLanguage = targetLanguage;
        this.completed = false;
    }

    /**
     * Returns the unique job identifier.
     * @return the job identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the target language for this job.
     * @return the target language code.
     */
    public String getTargetLanguage() {
        return targetLanguage;
    }

    /**
     * Returns the total number of items to be processed.
     * @return the total item count.
     */
    public int getTotalItems() {
        return totalItems;
    }

    /**
     * Sets the total number of items to be processed.
     * @param totalItems the total item count.
     */
    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    /**
     * Returns the number of items already processed.
     * @return the processed item count.
     */
    public int getProcessedItems() {
        return processedItems;
    }

    /**
     * Sets the number of items already processed.
     * @param processedItems the processed item count.
     */
    public void setProcessedItems(int processedItems) {
        this.processedItems = processedItems;
    }

    /**
     * Returns the generated Excel result data.
     * @return the byte array of the Excel file.
     */
    public byte[] getResultData() {
        return resultData;
    }

    /**
     * Sets the generated Excel result data.
     * @param resultData the byte array of the Excel file.
     */
    public void setResultData(byte[] resultData) {
        this.resultData = resultData;
    }

    /**
     * Returns whether the job has completed.
     * @return true if completed, false otherwise.
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Sets whether the job has completed.
     * @param completed completion status.
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    /**
     * Returns the error message if the job failed.
     * @return the error message or null.
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the error message if the job failed.
     * @param error the error message.
     */
    public void setError(String error) {
        this.error = error;
    }
}
