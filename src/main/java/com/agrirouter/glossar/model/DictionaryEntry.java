package com.agrirouter.glossar.model;

/**
 * Entry from the master dictionary.
 */
public class DictionaryEntry {
    private String identifier;
    private String german;
    private String targetLanguage;

    public DictionaryEntry(String identifier, String german, String targetLanguage) {
        this.identifier = identifier;
        this.german = german;
        this.targetLanguage = targetLanguage;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getGerman() {
        return german;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    @Override
    public String toString() {
        return "DictionaryEntry{" +
                "identifier='" + identifier + '\'' +
                ", german='" + german + '\'' +
                ", targetLanguage='" + targetLanguage + '\'' +
                '}';
    }
}
