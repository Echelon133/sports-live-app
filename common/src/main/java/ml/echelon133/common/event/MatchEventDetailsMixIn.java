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
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MatchEventDetails.StatusDto.class, name = "STATUS"),
})
public abstract class MatchEventDetailsMixIn {
}
