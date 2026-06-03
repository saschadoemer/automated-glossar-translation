package de.saschadoemer.glossar.translation.model;

import java.util.Objects;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DictionaryEntry that = (DictionaryEntry) o;
        return Objects.equals(identifier, that.identifier) &&
                Objects.equals(german, that.german) &&
                Objects.equals(targetLanguage, that.targetLanguage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, german, targetLanguage);
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
