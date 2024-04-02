package ml.echelon133.common.event.dto.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import ml.echelon133.common.event.dto.MatchEventDetails;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Serializer of {@link MatchEventDetails} used by Kafka.
 */
public class MatchEventDetailsSerializer implements Serializer<MatchEventDetails> {

    private static final ObjectMapper mapper = KafkaObjectMapper.getInstance();
    private final Logger logger = LoggerFactory.getLogger(MatchEventDetailsSerializer.class);

    @Override
    public byte[] serialize(String s, MatchEventDetails matchEventDetails) {
        try {
            if (matchEventDetails == null) {
                logger.warn("null passed to the serializer");
                return null;
            }
            return mapper.writeValueAsBytes(matchEventDetails);
        } catch (IOException e) {
            throw new SerializationException("failed to serialize MatchEventDetails to byte[]: " + e.getMessage());
        }
    }
}
