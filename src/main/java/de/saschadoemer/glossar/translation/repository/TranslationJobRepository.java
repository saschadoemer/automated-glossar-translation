package de.saschadoemer.glossar.translation.repository;

import de.saschadoemer.glossar.translation.model.TranslationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TranslationJob.
 */
@Repository
public interface TranslationJobRepository extends JpaRepository<TranslationJob, String> {

    /**
     * Finds all translation jobs that are not completed.
     *
     * @param completed the completion status.
     * @return a list of translation jobs.
     */
    List<TranslationJob> findAllByCompleted(boolean completed);

    /**
     * Deletes all translation jobs that match the given completion status.
     *
     * @param completed the completion status.
     */
    void deleteAllByCompleted(boolean completed);

}
