package ml.echelon133.common.event.dto.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import ml.echelon133.common.event.MatchEventDetailsMixIn;
import ml.echelon133.common.event.dto.MatchEventDetails;

/**
 * {@link ObjectMapper} used by Kafka to serialize/deserialize {@link MatchEventDetails}.
 */
public class KafkaObjectMapper {

    private KafkaObjectMapper() {}

    private static class Loader {
        static final ObjectMapper INSTANCE;

        static {
            INSTANCE = new ObjectMapper();
            INSTANCE.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            INSTANCE.addMixIn(MatchEventDetails.class, MatchEventDetailsMixIn.class);
        }
    }

    public static ObjectMapper getInstance() {
        return Loader.INSTANCE;
    }
}
