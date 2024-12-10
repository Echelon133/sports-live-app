package ml.echelon133.matchservice.event.model;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ml.echelon133.common.event.dto.MatchEventDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StringToEventDetailsConverter implements AttributeConverter<MatchEventDetails, String> {

    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(StringToEventDetailsConverter.class.getName());

    @Autowired
    public StringToEventDetailsConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String convertToDatabaseColumn(MatchEventDetails baseEventDto) {
        String result = "";
        try {
            result = objectMapper.writeValueAsString(baseEventDto);
        } catch (JsonProcessingException ex) {
            logger.error("failed serialization of event of type " + baseEventDto.getClass());
        }
        return result;
    }

    @Override
    public MatchEventDetails convertToEntityAttribute(String s) {
        MatchEventDetails eventDto = null;
        try {
            eventDto = objectMapper.readValue(s, new TypeReference<MatchEventDetails>() {});
        } catch (JsonProcessingException ex) {
            logger.error("failed deserialization of event with content " + s);
        }
        return eventDto;
    }
}
