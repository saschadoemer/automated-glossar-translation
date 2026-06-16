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

    @Schema(hidden = true)
    @JsonIgnore
    @Lob
    @Column(columnDefinition = "BLOB")
    private byte[] correctedResultData;

    @Schema(description = "Whether the job has completed", example = "false")
    private boolean completed;

    @Schema(description = "Whether fuzzy matching was used", example = "false")
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean fuzzy = false;

    @Schema(description = "Maximum number of records to process", example = "100")
    private Integer threshold;

    @Schema(description = "Wait time in seconds between records", example = "3")
    @Column(nullable = false, columnDefinition = "int default 3")
    private int waitTime = 3;

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

    public TranslationJob(String id, String targetLanguage, boolean fuzzy, Integer threshold, int waitTime) {
        this.id = id;
        this.targetLanguage = targetLanguage;
        this.fuzzy = fuzzy;
        this.threshold = threshold;
        this.waitTime = waitTime;
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
     * Returns the corrected Excel result data.
     * @return the byte array of the corrected Excel file.
     */
    public byte[] getCorrectedResultData() {
        return correctedResultData;
    }

    /**
     * Sets the corrected Excel result data.
     * @param correctedResultData the byte array of the corrected Excel file.
     */
    public void setCorrectedResultData(byte[] correctedResultData) {
        this.correctedResultData = correctedResultData;
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
     * Returns whether fuzzy matching was used.
     * @return true if fuzzy matching was used.
     */
    public boolean isFuzzy() {
        return fuzzy;
    }

    /**
     * Sets whether fuzzy matching was used.
     * @param fuzzy fuzzy matching status.
     */
    public void setFuzzy(boolean fuzzy) {
        this.fuzzy = fuzzy;
    }

    /**
     * Returns the threshold for the number of records to process.
     * @return the threshold.
     */
    public Integer getThreshold() {
        return threshold;
    }

    /**
     * Sets the threshold for the number of records to process.
     * @param threshold the threshold.
     */
    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }

    /**
     * Returns the wait time in seconds between records.
     * @return the wait time.
     */
    public int getWaitTime() {
        return waitTime;
    }

    /**
     * Sets the wait time in seconds between records.
     * @param waitTime the wait time.
     */
    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
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
