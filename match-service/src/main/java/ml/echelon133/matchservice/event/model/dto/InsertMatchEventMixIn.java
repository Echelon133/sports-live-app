package ml.echelon133.matchservice.event.model.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * MixIn containing information about how to deserialize JSON objects into subclasses
 * of {@link InsertMatchEvent}.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = InsertMatchEvent.CardDto.class, name = "CARD"),
        @JsonSubTypes.Type(value = InsertMatchEvent.CommentaryDto.class, name = "COMMENTARY"),
        @JsonSubTypes.Type(value = InsertMatchEvent.GoalDto.class, name = "GOAL"),
        @JsonSubTypes.Type(value = InsertMatchEvent.PenaltyDto.class, name = "PENALTY"),
        @JsonSubTypes.Type(value = InsertMatchEvent.StatusDto.class, name = "STATUS"),
        @JsonSubTypes.Type(value = InsertMatchEvent.SubstitutionDto.class, name = "SUBSTITUTION"),
})
public abstract class InsertMatchEventMixIn {
}
