package ml.echelon133.common.event.dto;

import ml.echelon133.common.event.MatchEventType;

import java.util.UUID;

/**
 * Data class representing already processed match event of type <b>GOAL</b>.
 */
public record GoalEventDetailsDto(
        String minute,
        UUID competitionId,
        UUID teamId,
        SerializedPlayer scoringPlayer,
        SerializedPlayer assistingPlayer,
        boolean ownGoal
) implements MatchEventDetails {

    @Override
    public String type() {
        return MatchEventType.GOAL.name();
    }
}
