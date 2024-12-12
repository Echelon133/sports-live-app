package ml.echelon133.matchservice.event.model.dto;

import jakarta.validation.constraints.NotNull;
import ml.echelon133.common.event.MatchEventType;
import ml.echelon133.matchservice.event.model.dto.constraints.EventMinuteFormat;
import ml.echelon133.matchservice.event.model.dto.constraints.MatchStatusValid;

/**
 * Represents a change of the status of a match (e.g. by default, the initial status of the match
 * is set to <i>NOT_STARTED</i>; setting it to <i>FIRST_HALF</i> begins the match).
 *
 * @param minute minute of the match when the event happened
 * @param targetStatus desired status of the match
 */
public record UpsertStatusEventDto (
    @NotNull @EventMinuteFormat String minute,
    @NotNull @MatchStatusValid String targetStatus
) implements UpsertMatchEvent  {

    @Override
    public String type() {
        return MatchEventType.STATUS.name();
    }
}
