package de.saschadoemer.glossar.translation.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.util.Map;
import java.util.Objects;

/**
 * Entry from the master dictionary.
 */
@Schema(description = "Represents an entry from the master dictionary")
@Entity
@Table(name = "dictionary_entries")
public class DictionaryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "Unique identifier for the entry", example = "Identifier")
    @Column(columnDefinition = "TEXT")
    private String identifier;

    @Schema(description = "German translation for the entry", example = "Bezeichner")
    @Column(columnDefinition = "TEXT")
    private String german;

    @Schema(description = "Available translations for different language codes")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dictionary_translations", joinColumns = @JoinColumn(name = "dictionary_entry_id"))
    @MapKeyColumn(name = "language_code")
    @Column(name = "translation", columnDefinition = "TEXT")
    private Map<String, String> translations;

    public DictionaryEntry() {
    }

    public DictionaryEntry(String identifier, String german, Map<String, String> translations) {
        this.identifier = identifier;
        this.german = german;
        this.translations = translations;
    }

    /**
     * Returns the internal database identifier.
     * @return the id.
     */
    public Long getId() {
        return id;
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
