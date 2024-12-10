package ml.echelon133.common.event.dto;

import ml.echelon133.common.event.MatchEventType;

import java.util.UUID;

/**
 * Data class representing already processed match event of type <b>CARD</b>.
 */
public record CardEventDetailsDto(
        String minute,
        UUID competitionId,
        UUID teamId,
        CardType cardType,
        SerializedPlayer cardedPlayer
) implements MatchEventDetails {

    public enum CardType {
        YELLOW, SECOND_YELLOW, DIRECT_RED
    }

    @Override
    public String type() {
        return MatchEventType.CARD.name();
    }
}
