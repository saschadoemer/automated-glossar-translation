package de.saschadoemer.glossar.translation.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationJobTest {

    @Test
    void testJsonSerializationIgnoresResultData() throws Exception {
        TranslationJob job = new TranslationJob("job-123", "en-US");
        job.setResultData("some binary data".getBytes());
        
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(job);
        
        assertTrue(json.contains("job-123"));
        assertTrue(json.contains("en-US"));
        assertFalse(json.contains("resultData"));
    }
}
