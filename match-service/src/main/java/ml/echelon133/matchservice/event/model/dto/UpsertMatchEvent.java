package ml.echelon133.matchservice.event.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Common interface between all records which represent match events received from the client
 * (deserialized from JSON provided in the http request).
 * <p>
 *     Each record describes a type of match event
 *     (one record per {@link ml.echelon133.common.event.MatchEventType} enum value).
 * </p>
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        visible = true,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = UpsertCardEventDto.class, name = "CARD"),
        @JsonSubTypes.Type(value = UpsertCommentaryEventDto.class, name = "COMMENTARY"),
        @JsonSubTypes.Type(value = UpsertGoalEventDto.class, name = "GOAL"),
        @JsonSubTypes.Type(value = UpsertPenaltyEventDto.class, name = "PENALTY"),
        @JsonSubTypes.Type(value = UpsertStatusEventDto.class, name = "STATUS"),
        @JsonSubTypes.Type(value = UpsertSubstitutionEventDto.class, name = "SUBSTITUTION"),
})
// Ignoring "type" property during deserialization is required to avoid "UnrecognizedPropertyException"
// caused by the deserializer trying to insert "type" property into the record, without the "knowledge" that
// this property only exists in the serialized record for the sake of later deserialization, and is no longer needed
// after that.
@JsonIgnoreProperties(value = "type")
public sealed interface UpsertMatchEvent permits
        UpsertCardEventDto, UpsertCommentaryEventDto, UpsertGoalEventDto,
        UpsertPenaltyEventDto, UpsertStatusEventDto, UpsertSubstitutionEventDto
{}
