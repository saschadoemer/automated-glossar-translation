package de.saschadoemer.glossar.translation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;

/**
 * Service to manage the application state for resuming.
 */
@Service
public class StateService {

    private static final Logger logger = LoggerFactory.getLogger(StateService.class);
    private static final String STATE_FILE = ".state";

    /**
     * Loads the last processed ID for a given language.
     * @param language the language code.
     * @return the last processed ID, or null if not found.
     */
    public String loadLastProcessedId(String language) {
        var path = Paths.get(STATE_FILE + "_" + language);
        if (Files.exists(path)) {
            try {
                var id = Files.readString(path).trim();
                if (!id.isEmpty()) {
                    logger.info("Found previous state for {}. Last processed ID: {}", language, id);
                    return id;
                }
            } catch (IOException e) {
                logger.error("Error reading state file", e);
            }
        }
        return null;
    }

    /**
     * Saves the last processed ID for a given language.
     * @param id the ID to save.
     * @param language the language code.
     */
    public void saveLastProcessedId(String id, String language) {
        try {
            Files.writeString(Paths.get(STATE_FILE + "_" + language), id);
        } catch (IOException e) {
            logger.error("Error writing state file", e);
        }
    }

    /**
     * Clears the state for a given language.
     * @param language the language code.
     */
    public void clear(String language) {
        try {
            Files.deleteIfExists(Paths.get(STATE_FILE + "_" + language));
            logger.debug("State file for {} cleared.", language);
        } catch (IOException e) {
            logger.error("Error deleting state file", e);
        }
    }
}
