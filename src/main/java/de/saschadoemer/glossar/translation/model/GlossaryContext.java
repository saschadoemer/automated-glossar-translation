package de.saschadoemer.glossar.translation.model;

import java.util.List;

/**
 * Context gathered for a glossary entry, holding all exactly matching dictionary entries.
 */
public class GlossaryContext {
    private final String term;
    private final List<DictionaryEntry> matchingEntries;

    public GlossaryContext(String term, List<DictionaryEntry> matchingEntries) {
        this.term = term;
        this.matchingEntries = matchingEntries;
    }

    public String getTerm() {
        return term;
    }

    public List<DictionaryEntry> getMatchingEntries() {
        return matchingEntries;
    }

    @Override
    public String toString() {
        return "GlossaryContext{" +
                "term='" + term + '\'' +
                ", matchingEntries=" + matchingEntries +
                '}';
    }
}
