package de.saschadoemer.glossar.translation.model;

/**
 * Model representing a translation job.
 */
public class TranslationJob {
    private final String id;
    private final String targetLanguage;
    private int totalItems;
    private int processedItems;
    private byte[] resultData;
    private boolean completed;
    private String error;

    public TranslationJob(String id, String targetLanguage) {
        this.id = id;
        this.targetLanguage = targetLanguage;
        this.completed = false;
    }

    public String getId() {
        return id;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getProcessedItems() {
        return processedItems;
    }

    public void setProcessedItems(int processedItems) {
        this.processedItems = processedItems;
    }

    public byte[] getResultData() {
        return resultData;
    }
    
    public void setResultData(byte[] resultData) {
        this.resultData = resultData;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
