package ml.echelon133.matchservice.match.model;

import ml.echelon133.common.event.MatchEventType;
import ml.echelon133.common.match.MatchResult;
import ml.echelon133.common.match.MatchStatus;

import java.io.Serializable;
import java.util.UUID;

/**
 * Object representing the most important match events which are broadcast globally via websocket.
 * These events contain information used for updating compact entries of matches (i.e. changing the match status,
 * updating scorelines, or red card counters).
 */
public abstract class GlobalMatchEventDto implements Serializable {
    private UUID matchId;
    private MatchEventType type;

    private GlobalMatchEventDto(UUID matchId, MatchEventType type) {
        this.matchId = matchId;
        this.type = type;
    }

    public enum EventSide {
        HOME, AWAY
    }

    public UUID getMatchId() {
        return matchId;
    }

    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }

    public MatchEventType getType() {
        return type;
    }

    public void setType(MatchEventType type) {
        this.type = type;
    }

    /**
     * Represents a goal scored in a match.
     */
    public static class GoalEvent extends GlobalMatchEventDto {
        private EventSide side;

        public GoalEvent(UUID matchId, EventSide side) {
            super(matchId, MatchEventType.GOAL);
            this.side = side;
        }

        public EventSide getSide() {
            return side;
        }

        public void setSide(EventSide side) {
            this.side = side;
        }
    }

    /**
     * Represents a red card given in a match.
     */
    public static class RedCardEvent extends GlobalMatchEventDto {
        private EventSide side;

        public RedCardEvent(UUID matchId, EventSide side) {
            super(matchId, MatchEventType.CARD);
            this.side = side;
        }

        public EventSide getSide() {
            return side;
        }

        public void setSide(EventSide side) {
            this.side = side;
        }
    }

    /**
     * Represents a change of match's status.
     */
    public static class StatusEvent extends GlobalMatchEventDto {
        private MatchStatus targetStatus;
        private MatchResult result;

        public StatusEvent(UUID matchId, MatchStatus targetStatus, MatchResult result) {
            super(matchId, MatchEventType.STATUS);
            this.targetStatus = targetStatus;
            this.result = result;
        }

        public MatchStatus getTargetStatus() {
            return targetStatus;
        }

        public void setTargetStatus(MatchStatus targetStatus) {
            this.targetStatus = targetStatus;
        }

        public MatchResult getResult() {
            return result;
        }

        public void setResult(MatchResult result) {
            this.result = result;
        }
    }
}
