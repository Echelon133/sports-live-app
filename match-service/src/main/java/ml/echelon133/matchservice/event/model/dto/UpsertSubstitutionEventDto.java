package ml.echelon133.matchservice.event.model.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import ml.echelon133.common.event.MatchEventType;
import ml.echelon133.matchservice.event.model.dto.constraints.EventMinuteFormat;
import ml.echelon133.matchservice.event.model.dto.constraints.SubstitutionPlayerIdsDifferent;
import ml.echelon133.matchservice.team.constraints.TeamPlayerExists;

/**
 * Represents a substitution of players.
 *
 * @param minute minute of the match when the event happened
 * @param playerInId id of the player coming on the pitch
 * @param playerOutId id of the player coming off the pitch
 */
@JsonTypeName(value = "SUBSTITUTION")
@SubstitutionPlayerIdsDifferent
public record UpsertSubstitutionEventDto(
    @NotNull @EventMinuteFormat String minute,
    @NotNull @TeamPlayerExists String playerInId,
    @NotNull @TeamPlayerExists String playerOutId
) implements UpsertMatchEvent {

    @Override
    public String type() {
        return MatchEventType.SUBSTITUTION.name();
    }
}
