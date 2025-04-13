package ml.echelon133.common.event.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

/**
 * Event used for synchronizing the match state between services.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MatchInfo.CreationEvent.class, name = "CREATION"),
        @JsonSubTypes.Type(value = MatchInfo.FinishEvent.class, name = "FINISH"),
})
@JsonIgnoreProperties(value = "type")
public sealed interface MatchInfo permits MatchInfo.CreationEvent, MatchInfo.FinishEvent {

    enum EventType {
        CREATION,
        FINISH
    }

    String type();
    UUID competitionId();
    UUID matchId();

    record CreationEvent(UUID competitionId, UUID matchId) implements MatchInfo {
        @Override
        public String type() {
            return EventType.CREATION.name();
        }
    }

    record FinishEvent(UUID competitionId, UUID matchId) implements MatchInfo {
        @Override
        public String type() {
            return EventType.FINISH.name();
        }
    }
}
