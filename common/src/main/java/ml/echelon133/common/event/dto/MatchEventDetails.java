package ml.echelon133.common.event.dto;

import ml.echelon133.common.event.MatchEventType;
import ml.echelon133.common.match.MatchStatus;

import java.io.Serializable;
import java.util.UUID;

/**
 * Data class which encapsulates detailed information about a match event that has already been processed.
 *
 * There are multiple types of events, each type has a different number of fields of various types.
 * Serialization of this class lets us easily store these complex events in a single database column.
 */
public abstract class MatchEventDetails implements Serializable {

    private MatchEventType type;
    private String minute;
    private UUID competitionId;

    public MatchEventDetails() {}
    public MatchEventDetails(MatchEventType type, String minute, UUID competitionId) {
        this.type = type;
        this.minute = minute;
        this.competitionId = competitionId;
    }

    public MatchEventType getType() {
        return type;
    }

    public void setType(MatchEventType type) {
        this.type = type;
    }

    public String getMinute() {
        return minute;
    }

    public void setMinute(String minute) {
        this.minute = minute;
    }

    public UUID getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(UUID competitionId) {
        this.competitionId = competitionId;
    }


    public static class SerializedPlayerInfo implements Serializable {
        private UUID teamPlayerId;
        private UUID playerId;
        private String name;

        public SerializedPlayerInfo() {}
        public SerializedPlayerInfo(UUID teamPlayerId, UUID playerId, String name) {
            this.teamPlayerId = teamPlayerId;
            this.playerId = playerId;
            this.name = name;
        }

        public UUID getTeamPlayerId() {
            return teamPlayerId;
        }

        public void setTeamPlayerId(UUID teamPlayerId) {
            this.teamPlayerId = teamPlayerId;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public void setPlayerId(UUID playerId) {
            this.playerId = playerId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * Data class representing already processed match event of type <b>STATUS</b>.
     */
    public static class StatusDto extends MatchEventDetails {
        private MatchStatus targetStatus;

        public StatusDto() {}
        public StatusDto(String minute, UUID competitionId, MatchStatus targetStatus) {
            super(MatchEventType.STATUS, minute, competitionId);
            this.targetStatus = targetStatus;
        }

        public MatchStatus getTargetStatus() {
            return targetStatus;
        }

        public void setTargetStatus(MatchStatus targetStatus) {
            this.targetStatus = targetStatus;
        }
    }

    /**
     * Data class representing already processed match event of type <b>COMMENTARY</b>.
     */
    public static class CommentaryDto extends MatchEventDetails {
        private String message;

        public CommentaryDto() {}
        public CommentaryDto(String minute, UUID competitionId, String message) {
            super(MatchEventType.COMMENTARY, minute, competitionId);
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
     * Data class representing already processed match event of type <b>CARD</b>.
     */
    public static class CardDto extends MatchEventDetails {
        private UUID teamId;
        private CardType cardType;
        private SerializedPlayerInfo cardedPlayer;

        public enum CardType {
            YELLOW, SECOND_YELLOW, DIRECT_RED
        }

        public CardDto() {}
        public CardDto(String minute, UUID competitionId, UUID teamId, CardType cardType, SerializedPlayerInfo cardedPlayer) {
            super(MatchEventType.CARD, minute, competitionId);
            this.teamId = teamId;
            this.cardType = cardType;
            this.cardedPlayer = cardedPlayer;
        }


        public UUID getTeamId() {
            return teamId;
        }

        public void setTeamId(UUID teamId) {
            this.teamId = teamId;
        }

        public CardType getCardType() {
            return cardType;
        }

        public void setCardType(CardType cardType) {
            this.cardType = cardType;
        }

        public SerializedPlayerInfo getCardedPlayer() {
            return cardedPlayer;
        }

        public void setCardedPlayer(SerializedPlayerInfo cardedPlayer) {
            this.cardedPlayer = cardedPlayer;
        }
    }

    /**
     * Data class representing already processed match event of type <b>GOAL</b>.
     */
    public static class GoalDto extends MatchEventDetails {
        private UUID teamId;
        private SerializedPlayerInfo scoringPlayer;
        private SerializedPlayerInfo assistingPlayer;
        private boolean ownGoal;

        public GoalDto() {}
        public GoalDto(
                String minute, UUID competitionId, UUID teamId,
                SerializedPlayerInfo scoringPlayer, SerializedPlayerInfo assistingPlayer, boolean ownGoal
        ) {
            super(MatchEventType.GOAL, minute, competitionId);
            this.teamId = teamId;
            this.scoringPlayer = scoringPlayer;
            this.assistingPlayer = assistingPlayer;
            this.ownGoal = ownGoal;
        }

        public UUID getTeamId() {
            return teamId;
        }

        public void setTeamId(UUID teamId) {
            this.teamId = teamId;
        }

        public SerializedPlayerInfo getScoringPlayer() {
            return scoringPlayer;
        }

        public void setScoringPlayer(SerializedPlayerInfo scoringPlayer) {
            this.scoringPlayer = scoringPlayer;
        }

        public SerializedPlayerInfo getAssistingPlayer() {
            return assistingPlayer;
        }

        public void setAssistingPlayer(SerializedPlayerInfo assistingPlayer) {
            this.assistingPlayer = assistingPlayer;
        }

        public boolean isOwnGoal() {
            return ownGoal;
        }

        public void setOwnGoal(boolean ownGoal) {
            this.ownGoal = ownGoal;
        }
    }

    /**
     * Data class representing already processed match event of type <b>SUBSTITUTION</b>.
     */
    public static class SubstitutionDto extends MatchEventDetails {
        private UUID teamId;
        private SerializedPlayerInfo playerIn;
        private SerializedPlayerInfo playerOut;

        public SubstitutionDto() {}
        public SubstitutionDto(
                String minute, UUID competitionId, UUID teamId,
                SerializedPlayerInfo playerIn, SerializedPlayerInfo playerOut
        ) {
            super(MatchEventType.GOAL, minute, competitionId);
            this.teamId = teamId;
            this.playerIn = playerIn;
            this.playerOut = playerOut;
        }

        public UUID getTeamId() {
            return teamId;
        }

        public void setTeamId(UUID teamId) {
            this.teamId = teamId;
        }

        public SerializedPlayerInfo getPlayerIn() {
            return playerIn;
        }

        public void setPlayerIn(SerializedPlayerInfo playerIn) {
            this.playerIn = playerIn;
        }

        public SerializedPlayerInfo getPlayerOut() {
            return playerOut;
        }

        public void setPlayerOut(SerializedPlayerInfo playerOut) {
            this.playerOut = playerOut;
        }
    }
}
