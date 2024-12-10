package ml.echelon133.matchservice.match.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

/**
 * Common interface between all records which represent the most important match events broadcast globally via websocket.
 *
 * <p>
 *     These events only carry the most important pieces of information, because they are used for updating
 *     the live view of lists of matches without having to refresh the browser.
 * </p>
 * <p>
 *     Each event's type name should correspond to one {@link ml.echelon133.common.event.MatchEventType} value.
 * </p>
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        visible = true,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GlobalGoalEventDto.class, name = "GOAL"),
        @JsonSubTypes.Type(value = GlobalStatusEventDto.class, name = "STATUS"),
        @JsonSubTypes.Type(value = GlobalRedCardEventDto.class, name = "CARD")
})
public sealed interface GlobalMatchEvent permits GlobalGoalEventDto, GlobalRedCardEventDto, GlobalStatusEventDto {
    enum EventSide {
        HOME, AWAY
    }

    String type();
    UUID matchId();
}
