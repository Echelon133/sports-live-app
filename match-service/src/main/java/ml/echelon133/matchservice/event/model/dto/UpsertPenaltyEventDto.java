package ml.echelon133.matchservice.event.model.dto;


import jakarta.validation.constraints.NotNull;
import ml.echelon133.common.event.MatchEventType;
import ml.echelon133.matchservice.event.model.dto.constraints.EventMinuteFormat;
import ml.echelon133.matchservice.team.constraints.TeamPlayerExists;

/**
 * Represents a penalty kick (both scored and missed).
 *
 * @param minute minute of the match when the event happened
 * @param shootingPlayerId id of the player shooting
 * @param scored `false` if the player missed, otherwise `true`
 */
public record UpsertPenaltyEventDto(
    @NotNull @EventMinuteFormat String minute,
    @NotNull @TeamPlayerExists String shootingPlayerId,
    boolean scored
) implements UpsertMatchEvent {

    @Override
    public String type() {
        return MatchEventType.PENALTY.name();
    }
}
