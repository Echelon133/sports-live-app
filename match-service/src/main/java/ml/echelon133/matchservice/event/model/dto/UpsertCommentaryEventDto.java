package ml.echelon133.matchservice.event.model.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ml.echelon133.common.event.MatchEventType;
import ml.echelon133.matchservice.event.model.dto.constraints.EventMinuteFormat;

/**
 * Represents the commentary of an event of the match.
 *
 * @param minute minute of the match when the event happened
 * @param message contents of the match commentary
 */
@JsonTypeName(value = "COMMENTARY")
public record UpsertCommentaryEventDto(
    @NotNull @EventMinuteFormat String minute,
    @NotNull @Size(min = 1, max = 1000, message = "should contain between {min} and {max} characters") String message
) implements UpsertMatchEvent {

    @Override
    public String type() {
        return MatchEventType.COMMENTARY.name();
    }
}
