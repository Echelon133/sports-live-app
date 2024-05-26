package ml.echelon133.common.event.dto.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import ml.echelon133.common.event.dto.MatchEventDetails;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Deserializer of {@link MatchEventDetails} used by Kafka.
 */
public class MatchEventDetailsDeserializer implements Deserializer<MatchEventDetails> {

    private static final ObjectMapper mapper = KafkaObjectMapper.getInstance();
    private final Logger logger = LoggerFactory.getLogger(MatchEventDetailsDeserializer.class);

    @Override
    public MatchEventDetails deserialize(String s, byte[] bytes) {
        try {
            if (bytes == null) {
                logger.warn("unexpected null received by the deserializer");
                return null;
            }
            return mapper.readValue(bytes, MatchEventDetails.class);
        } catch (IOException e) {
            throw new SerializationException("failed to deserialize byte[] to MatchEventDetails: " + e.getMessage());
        }
    }
}
