package de.saschadoemer.glossar.translation.model;

import java.util.Map;
import java.util.Objects;

/**
 * Entry from the master dictionary.
 */
public class DictionaryEntry {
    private String identifier;
    private String german;
    private Map<String, String> translations;

    public DictionaryEntry(String identifier, String german, Map<String, String> translations) {
        this.identifier = identifier;
        this.german = german;
        this.translations = translations;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getGerman() {
        return german;
    }

    public Map<String, String> getTranslations() {
        return translations;
    }

    public String getTranslation(String languageCode) {
        return translations != null ? translations.get(languageCode) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DictionaryEntry that = (DictionaryEntry) o;
        return Objects.equals(identifier, that.identifier) &&
                Objects.equals(german, that.german) &&
                Objects.equals(translations, that.translations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, german, translations);
    }

    @Override
    public String toString() {
        return "DictionaryEntry{" +
                "identifier='" + identifier + '\'' +
                ", german='" + german + '\'' +
                ", translations=" + translations +
                '}';
    }
}
