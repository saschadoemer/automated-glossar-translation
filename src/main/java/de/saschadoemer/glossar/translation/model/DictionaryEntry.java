package de.saschadoemer.glossar.translation.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.Objects;

/**
 * Entry from the master dictionary.
 */
@Schema(description = "Represents an entry from the master dictionary")
public class DictionaryEntry {

    @Schema(description = "Unique identifier for the entry", example = "Identifier")
    private final String identifier;

    @Schema(description = "German translation for the entry", example = "Bezeichner")
    private final String german;

    @Schema(description = "Available translations for different language codes")
    private final Map<String, String> translations;

    public DictionaryEntry(String identifier, String german, Map<String, String> translations) {
        this.identifier = identifier;
        this.german = german;
        this.translations = translations;
    }

    /**
     * Returns the unique identifier for the entry.
     * @return the identifier.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns the German translation for the entry.
     * @return the German translation.
     */
    public String getGerman() {
        return german;
    }

    /**
     * Returns all available translations.
     * @return a map of language codes to translations.
     */
    public Map<String, String> getTranslations() {
        return translations;
    }

    /**
     * Returns the translation for a specific language code.
     * @param languageCode the language code to look for.
     * @return the translation, or null if not found.
     */
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
