package ml.echelon133.matchservice.event.model.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import ml.echelon133.common.event.MatchEventType;
import ml.echelon133.matchservice.event.model.dto.constraints.EventMinuteFormat;
import ml.echelon133.matchservice.event.model.dto.constraints.GoalPlayerIdsDifferent;
import ml.echelon133.matchservice.team.constraints.TeamPlayerExists;

/**
 * Represents a goal (or an own goal).
 * <p>
 *   A goal can, but does not have to have a player assisting. In case of an own goal, the player assisting should not be set,
 *   and if it's set, the event must be discarded.
 * </p>
 *
 * @param minute minute of the match when the event happened
 * @param scoringPlayerId id of the goalscoring player
 * @param assistingPlayerId (optional) id of the assisting player
 * @param ownGoal `true` if the event represents an own goal
 */
@JsonTypeName(value = "GOAL")
@GoalPlayerIdsDifferent
public record UpsertGoalEventDto(
        @NotNull @EventMinuteFormat String minute,
        @NotNull @TeamPlayerExists String scoringPlayerId,
        @TeamPlayerExists String assistingPlayerId,
        boolean ownGoal
) implements UpsertMatchEvent {

    @Override
    public String type() {
        return MatchEventType.GOAL.name();
    }
}
