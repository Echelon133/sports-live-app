package ml.echelon133.matchservice.match.model;

import ml.echelon133.common.event.MatchEventType;
import ml.echelon133.common.match.MatchResult;
import ml.echelon133.common.match.MatchStatus;

import java.util.UUID;

/**
 * Represents a change of match's status, broadcast via websocket.
 *
 * @param matchId id of the match whose status changed
 * @param targetStatus new status of the match
 * @param result current result of the match
 */
public record GlobalStatusEventDto(UUID matchId, MatchStatus targetStatus, MatchResult result) implements GlobalMatchEvent {

    @Override
    public String type() {
        return MatchEventType.STATUS.name();
    }
}
