package de.saschadoemer.glossar.translation.repository;

import de.saschadoemer.glossar.translation.model.DictionaryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for DictionaryEntry.
 */
@Repository
public interface DictionaryEntryRepository extends JpaRepository<DictionaryEntry, Long> {

    List<DictionaryEntry> findByIdentifier(String identifier);

    List<DictionaryEntry> findByGerman(String german);
    
    List<DictionaryEntry> findByIdentifierOrGerman(String identifier, String german);
}
