package ml.echelon133.matchservice.match.model;

import ml.echelon133.common.event.MatchEventType;

import java.util.UUID;

/**
 * Represents a goal scored in a match, broadcast via websocket.
 *
 * @param matchId id of the match in which a goal happened
 * @param side side which scored the goal
 */
public record GlobalGoalEventDto(UUID matchId, GlobalMatchEvent.EventSide side) implements GlobalMatchEvent {

    @Override
    public String type() {
        return MatchEventType.GOAL.name();
    }
}
