package ml.echelon133.common.event.dto.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import ml.echelon133.common.event.dto.MatchEventDetails;

/**
 * {@link ObjectMapper} used for serialization/deserialization of Kafka messages.
 */
public class KafkaObjectMapper {

    private KafkaObjectMapper() {}

    private static class Loader {
        static final ObjectMapper INSTANCE;

        static {
            INSTANCE = new ObjectMapper();
            INSTANCE.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            INSTANCE.addMixIn(MatchEventDetails.class, MatchEventDetails.class);
        }
    }

    public static ObjectMapper getInstance() {
        return Loader.INSTANCE;
    }
}
