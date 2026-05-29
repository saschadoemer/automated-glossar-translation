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
     * @return The last processed ID, or null if no state exists.
     */
    public String loadLastProcessedId() {
        Path path = Paths.get(STATE_FILE);
        if (Files.exists(path)) {
            try {
                String id = Files.readString(path).trim();
                if (!id.isEmpty()) {
                    logger.info("Found previous state. Last processed ID: {}", id);
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
     * @param id The ID to save.
     */
    public void saveLastProcessedId(String id) {
        try {
            Files.writeString(Paths.get(STATE_FILE), id);
        } catch (IOException e) {
            logger.error("Error writing state file", e);
        }
    }

    /**
     * Clears the state file.
     */
    public void clear() {
        try {
            Files.deleteIfExists(Paths.get(STATE_FILE));
            logger.debug("State file cleared.");
        } catch (IOException e) {
            logger.error("Error deleting state file", e);
        }
    }
}
