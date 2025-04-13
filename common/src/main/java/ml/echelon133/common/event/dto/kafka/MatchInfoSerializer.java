package ml.echelon133.common.event.dto.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import ml.echelon133.common.event.dto.MatchInfo;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Serializer of {@link ml.echelon133.common.event.dto.MatchInfo} used by Kafka.
 */
public class MatchInfoSerializer implements Serializer<MatchInfo> {

    private static final ObjectMapper mapper = KafkaObjectMapper.getInstance();
    private final Logger logger = LoggerFactory.getLogger(MatchInfoSerializer.class);

    @Override
    public byte[] serialize(String s, MatchInfo matchInfo) {
        try {
            if (matchInfo == null) {
                logger.warn("null passed to the serializer");
                return null;
            }
            return mapper.writeValueAsBytes(matchInfo);
        } catch (IOException e) {
            throw new SerializationException("failed to serialize MatchInfo to byte[]: " + e.getMessage());
        }
    }
}
