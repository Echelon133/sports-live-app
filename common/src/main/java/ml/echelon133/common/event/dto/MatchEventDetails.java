package ml.echelon133.common.event.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

/**
 * Common interface between all records which represent already processed match events (i.e. validated by
 * constraint validators, validated by the business logic, and saved in the database).
 *
 * <p>
 *     These records carry all information needed by a client of the API to display views of
 *     match events (i.e. minute of the event, team to which the event is important, players involved in
 *     the event, etc.).
 * </p>
 * <p>
 *     Each event's type name should correspond to one {@link ml.echelon133.common.event.MatchEventType} value.
 * </p>
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StatusEventDetailsDto.class, name = "STATUS"),
        @JsonSubTypes.Type(value = CommentaryEventDetailsDto.class, name = "COMMENTARY"),
        @JsonSubTypes.Type(value = CardEventDetailsDto.class, name = "CARD"),
        @JsonSubTypes.Type(value = GoalEventDetailsDto.class, name = "GOAL"),
        @JsonSubTypes.Type(value = SubstitutionEventDetailsDto.class, name = "SUBSTITUTION"),
        @JsonSubTypes.Type(value = PenaltyEventDetailsDto.class, name = "PENALTY")
})
// Ignoring "type" property during deserialization is required to avoid "UnrecognizedPropertyException"
// caused by the deserializer trying to insert "type" property into the record, without the "knowledge" that
// this property is not stored as a property on a record, and instead is only a constant accessible via
// an accessor method.
@JsonIgnoreProperties(value = "type")
public sealed interface MatchEventDetails permits
    StatusEventDetailsDto, CommentaryEventDetailsDto, CardEventDetailsDto,
    GoalEventDetailsDto, SubstitutionEventDetailsDto, PenaltyEventDetailsDto
{
    String type();
    String minute();
    UUID competitionId();
}
