package ml.echelon133.common.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ml.echelon133.common.event.dto.MatchEventDetails;

/**
 * MixIn containing information about how to map subtypes of {@link MatchEventDetails} into JSON which preserves
 * the subtype information.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MatchEventDetails.StatusDto.class, name = "STATUS"),
        @JsonSubTypes.Type(value = MatchEventDetails.CommentaryDto.class, name = "COMMENTARY"),
        @JsonSubTypes.Type(value = MatchEventDetails.CardDto.class, name = "CARD"),
        @JsonSubTypes.Type(value = MatchEventDetails.GoalDto.class, name = "GOAL"),
        @JsonSubTypes.Type(value = MatchEventDetails.SubstitutionDto.class, name = "SUBSTITUTION"),
        @JsonSubTypes.Type(value = MatchEventDetails.PenaltyDto.class, name = "PENALTY")
})
public abstract class MatchEventDetailsMixIn {
}
