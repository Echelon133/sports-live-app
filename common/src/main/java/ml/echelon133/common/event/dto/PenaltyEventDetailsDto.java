package ml.echelon133.common.event.dto;

import ml.echelon133.common.event.MatchEventType;

import java.util.UUID;

/**
 * Data class representing already processed match event of type <b>PENALTY</b>.
 */
public record PenaltyEventDetailsDto(
        String minute,
        UUID competitionId,
        UUID teamId,
        SerializedPlayer shootingPlayer,
        boolean countAsGoal,
        boolean scored
) implements MatchEventDetails {

    @Override
    public String type() {
        return MatchEventType.PENALTY.name();
    }
}
