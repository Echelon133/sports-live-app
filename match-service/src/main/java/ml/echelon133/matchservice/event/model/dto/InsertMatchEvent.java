package ml.echelon133.matchservice.event.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import ml.echelon133.common.event.MatchEventType;
import ml.echelon133.matchservice.event.model.dto.constraints.EventMinuteFormat;
import ml.echelon133.matchservice.event.model.dto.constraints.MatchStatusValid;
import ml.echelon133.matchservice.team.constraints.TeamPlayerExists;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Data classes representing all pre-processed match events that can be provided by the client.
 */
public abstract class InsertMatchEvent {

    @NotNull
    @JsonProperty(value = "type")
    private String type;

    @NotNull
    @EventMinuteFormat
    private String minute;

    public InsertMatchEvent() {}
    public InsertMatchEvent(String type, String minute) {
        this.type = type;
        this.minute = minute;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMinute() {
        return minute;
    }

    public void setMinute(String minute) {
        this.minute = minute;
    }

    /**
     * Data class representing client-provided match event of type <b>STATUS</b>.
     */
    public static class StatusDto extends InsertMatchEvent {
        @NotNull
        @MatchStatusValid
        private String targetStatus;

        public StatusDto() {}
        public StatusDto(String minute, String targetStatus) {
            super(MatchEventType.STATUS.name(), minute);
            this.targetStatus = targetStatus;
        }

        public String getTargetStatus() {
            return targetStatus;
        }

        public void setTargetStatus(String targetStatus) {
            this.targetStatus = targetStatus;
        }
    }

    /**
     * Data class representing client-provided match event of type <b>COMMENTARY</b>.
     */
    public static class CommentaryDto extends InsertMatchEvent {
        @NotNull
        @Size(min = 1, max = 1000, message = "should contain between {min} and {max} characters")
        private String message;

        public CommentaryDto() {}
        public CommentaryDto(String minute, String message) {
            super(MatchEventType.COMMENTARY.name(), minute);
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * Data class representing client-provided match event of type <b>CARD</b>.
     */
    public static class CardDto extends InsertMatchEvent {
        @NotNull
        @TeamPlayerExists
        private String cardedPlayerId;

        private boolean redCard;

        public CardDto() {}
        public CardDto(String minute, String cardedPlayerId, boolean redCard) {
            super(MatchEventType.CARD.name(), minute);
            this.cardedPlayerId = cardedPlayerId;
            this.redCard = redCard;
        }

        public String getCardedPlayerId() {
            return cardedPlayerId;
        }

        public void setCardedPlayerId(String cardedPlayerId) {
            this.cardedPlayerId = cardedPlayerId;
        }

        public boolean isRedCard() {
            return redCard;
        }

        public void setRedCard(boolean redCard) {
            this.redCard = redCard;
        }
    }

    /**
     * Data class representing client-provided match event of type <b>GOAL</b>.
     */
    public static class GoalDto extends InsertMatchEvent {
        @NotNull
        @TeamPlayerExists
        private String scoringPlayerId;

        @TeamPlayerExists
        private String assistingPlayerId;

        private boolean ownGoal;

        public GoalDto() {}
        public GoalDto(String minute, String scoringPlayerId, String assistingPlayerId, boolean ownGoal) {
            super(MatchEventType.GOAL.name(), minute);
            this.scoringPlayerId = scoringPlayerId;
            this.assistingPlayerId = assistingPlayerId;
            this.ownGoal = ownGoal;
        }

        public String getScoringPlayerId() {
            return scoringPlayerId;
        }

        public void setScoringPlayerId(String scoringPlayerId) {
            this.scoringPlayerId = scoringPlayerId;
        }

        public String getAssistingPlayerId() {
            return assistingPlayerId;
        }

        public void setAssistingPlayerId(String assistingPlayerId) {
            this.assistingPlayerId = assistingPlayerId;
        }

        public boolean isOwnGoal() {
            return ownGoal;
        }

        public void setOwnGoal(boolean ownGoal) {
            this.ownGoal = ownGoal;
        }
    }

    /**
     * Data class representing client-provided match event of type <b>PENALTY</b>.
     */
    public static class PenaltyDto extends InsertMatchEvent {
        @NotNull
        @TeamPlayerExists
        private String shootingPlayerId;

        private boolean scored;

        public PenaltyDto() {}
        public PenaltyDto(String minute, String shootingPlayerId, boolean scored) {
            super(MatchEventType.PENALTY.name(), minute);
            this.shootingPlayerId = shootingPlayerId;
            this.scored = scored;
        }

        public String getShootingPlayerId() {
            return shootingPlayerId;
        }

        public void setShootingPlayerId(String shootingPlayerId) {
            this.shootingPlayerId = shootingPlayerId;
        }

        public boolean isScored() {
            return scored;
        }

        public void setScored(boolean scored) {
            this.scored = scored;
        }
    }

    /**
     * Data class representing client-provided match event of type <b>SUBSTITUTION</b>.
     */
    public static class SubstitutionDto extends InsertMatchEvent {
        @NotNull
        @TeamPlayerExists
        private String playerInId;

        @NotNull
        @TeamPlayerExists
        private String playerOutId;

        public SubstitutionDto() {}
        public SubstitutionDto(String minute, String playerInId, String playerOutId) {
            super(MatchEventType.SUBSTITUTION.name(), minute);
            this.playerInId = playerInId;
            this.playerOutId = playerOutId;
        }

        public String getPlayerInId() {
            return playerInId;
        }

        public void setPlayerInId(String playerInId) {
            this.playerInId = playerInId;
        }

        public String getPlayerOutId() {
            return playerOutId;
        }

        public void setPlayerOutId(String playerOutId) {
            this.playerOutId = playerOutId;
        }
    }
}
