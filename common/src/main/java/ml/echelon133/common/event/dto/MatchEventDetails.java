package ml.echelon133.common.event.dto;

import ml.echelon133.common.event.MatchEventType;
import ml.echelon133.common.match.MatchResult;
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

    /**
     * This field stores information needed to determine which subtype of `MatchEventDetails` is being
     * serialized/deserialized by an {@link com.fasterxml.jackson.databind.ObjectMapper}.
     * Every mapper which needs to work with subtypes of this class has to register
     * {@link ml.echelon133.common.event.MatchEventDetailsMixIn} as its mixin.
     *
     * This field is <b>NOT</b> a class tag. Only {@link com.fasterxml.jackson.databind.ObjectMapper}
     * should ever read its contents.
     */
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

    /**
     * Data class representing already processed match event of type <b>STATUS</b>.
     */
    public static class StatusDto extends MatchEventDetails {
        private MatchStatus targetStatus;
        private SerializedTeam teams;
        private MatchResult result;
        private SerializedScore mainScore;

        public StatusDto() {}
        public StatusDto(
                String minute,
                UUID competitionId,
                MatchStatus targetStatus,
                SerializedTeam teams,
                MatchResult matchResult,
                SerializedScore mainScore
        ) {
            super(MatchEventType.STATUS, minute, competitionId);
            this.teams = teams;
            this.targetStatus = targetStatus;
            this.result = matchResult;
            this.mainScore = mainScore;
        }

        public MatchStatus getTargetStatus() {
            return targetStatus;
        }

        public void setTargetStatus(MatchStatus targetStatus) {
            this.targetStatus = targetStatus;
        }

        public SerializedTeam getTeams() {
            return teams;
        }

        public void setTeams(SerializedTeam teams) {
            this.teams = teams;
        }

        public MatchResult getResult() {
            return result;
        }

        public void setResult(MatchResult result) {
            this.result = result;
        }

        public SerializedScore getMainScore() {
            return mainScore;
        }

        public void setMainScore(SerializedScore mainScore) {
            this.mainScore = mainScore;
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
        private SerializedPlayer cardedPlayer;

        public enum CardType {
            YELLOW, SECOND_YELLOW, DIRECT_RED
        }

        public CardDto() {}
        public CardDto(String minute, UUID competitionId, UUID teamId, CardType cardType, SerializedPlayer cardedPlayer) {
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

        public SerializedPlayer getCardedPlayer() {
            return cardedPlayer;
        }

        public void setCardedPlayer(SerializedPlayer cardedPlayer) {
            this.cardedPlayer = cardedPlayer;
        }
    }

    /**
     * Data class representing already processed match event of type <b>GOAL</b>.
     */
    public static class GoalDto extends MatchEventDetails {
        private UUID teamId;
        private SerializedPlayer scoringPlayer;
        private SerializedPlayer assistingPlayer;
        private boolean ownGoal;

        public GoalDto() {}
        public GoalDto(
                String minute, UUID competitionId, UUID teamId,
                SerializedPlayer scoringPlayer, SerializedPlayer assistingPlayer, boolean ownGoal
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

        public SerializedPlayer getScoringPlayer() {
            return scoringPlayer;
        }

        public void setScoringPlayer(SerializedPlayer scoringPlayer) {
            this.scoringPlayer = scoringPlayer;
        }

        public SerializedPlayer getAssistingPlayer() {
            return assistingPlayer;
        }

        public void setAssistingPlayer(SerializedPlayer assistingPlayer) {
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
        private SerializedPlayer playerIn;
        private SerializedPlayer playerOut;

        public SubstitutionDto() {}
        public SubstitutionDto(
                String minute, UUID competitionId, UUID teamId,
                SerializedPlayer playerIn, SerializedPlayer playerOut
        ) {
            super(MatchEventType.SUBSTITUTION, minute, competitionId);
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

        public SerializedPlayer getPlayerIn() {
            return playerIn;
        }

        public void setPlayerIn(SerializedPlayer playerIn) {
            this.playerIn = playerIn;
        }

        public SerializedPlayer getPlayerOut() {
            return playerOut;
        }

        public void setPlayerOut(SerializedPlayer playerOut) {
            this.playerOut = playerOut;
        }
    }

    /**
     * Data class representing already processed match event of type <b>PENALTY</b>.
     */
    public static class PenaltyDto extends MatchEventDetails {
        private UUID teamId;
        private SerializedPlayer shootingPlayer;
        private boolean countAsGoal;
        private boolean scored;

        public PenaltyDto() {}
        public PenaltyDto(
                String minute, UUID competitionId, UUID teamId,
                SerializedPlayer shootingPlayer, boolean countAsGoal, boolean scored
        ) {
            super(MatchEventType.PENALTY, minute, competitionId);
            this.teamId = teamId;
            this.shootingPlayer = shootingPlayer;
            this.countAsGoal = countAsGoal;
            this.scored = scored;
        }

        public UUID getTeamId() {
            return teamId;
        }

        public void setTeamId(UUID teamId) {
            this.teamId = teamId;
        }

        public SerializedPlayer getShootingPlayer() {
            return shootingPlayer;
        }

        public void setShootingPlayer(SerializedPlayer shootingPlayer) {
            this.shootingPlayer = shootingPlayer;
        }

        public boolean isCountAsGoal() {
            return countAsGoal;
        }

        public void setCountAsGoal(boolean countAsGoal) {
            this.countAsGoal = countAsGoal;
        }

        public boolean isScored() {
            return scored;
        }

        public void setScored(boolean scored) {
            this.scored = scored;
        }
    }
}
