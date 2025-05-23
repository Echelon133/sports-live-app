package ml.echelon133.matchservice.event.model.dto;

import jakarta.validation.constraints.NotNull;
import ml.echelon133.common.event.MatchEventType;
import ml.echelon133.matchservice.event.model.dto.constraints.EventMinuteFormat;
import ml.echelon133.matchservice.team.constraints.TeamPlayerExists;

/**
 * Represents a player getting yellow/red carded in the match.
 *
 * @param minute minute of the match when the event happened
 * @param cardedPlayerId id of the player who receives the card
 * @param redCard `true` if the card is red, otherwise the card is yellow
 */
public record UpsertCardEventDto(
    @NotNull @EventMinuteFormat String minute,
    @NotNull @TeamPlayerExists String cardedPlayerId,
    boolean redCard
) implements UpsertMatchEvent {

    @Override
    public String type() {
        return MatchEventType.CARD.name();
    }
}
