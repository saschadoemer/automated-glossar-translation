package de.saschadoemer.glossar.translation.repository;

import de.saschadoemer.glossar.translation.model.TranslationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for TranslationJob.
 */
@Repository
public interface TranslationJobRepository extends JpaRepository<TranslationJob, String> {
}
