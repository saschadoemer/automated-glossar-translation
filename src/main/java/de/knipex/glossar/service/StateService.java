package de.knipex.glossar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service to manage the application state for resuming.
 */
public class StateService {

    private static final Logger logger = LoggerFactory.getLogger(StateService.class);
    private static final String STATE_FILE = ".state";

    /**
     * Loads the last processed ID from the state file.
     *
     * @param language The language to load the state for.
     * @return The last processed ID, or null if no state exists.
     */
    public String loadLastProcessedId(String language) {
        Path path = Paths.get(STATE_FILE + "_" + language);
        if (Files.exists(path)) {
            try {
                String id = Files.readString(path).trim();
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
     * Saves the last processed ID to the state file.
     *
     * @param id       The ID to save.
     * @param language The language to save the state for.
     */
    public void saveLastProcessedId(String id, String language) {
        try {
            Files.writeString(Paths.get(STATE_FILE + "_" + language), id);
        } catch (IOException e) {
            logger.error("Error writing state file", e);
        }
    }

    /**
     * Clears the state file.
     *
     * @param language The language to clear the state for.
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
