package pl.echelon133.competitionservice.competition.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import java.io.Serializable;
import java.util.UUID;

@Entity
public class UnassignedMatch {

    @Embeddable
    public static class UnassignedMatchId implements Serializable {
        private UUID matchId;
        private UUID competitionId;
        public UnassignedMatchId() {}
        public UnassignedMatchId(UUID matchId, UUID competitionId) {
            this.matchId = matchId;
            this.competitionId = competitionId;
        }

        public UUID getMatchId() {
            return matchId;
        }

        public void setMatchId(UUID matchId) {
            this.matchId = matchId;
        }

        public UUID getCompetitionId() {
            return competitionId;
        }

        public void setCompetitionId(UUID competitionId) {
            this.competitionId = competitionId;
        }
    }

    @EmbeddedId
    private UnassignedMatchId id;
    private boolean assigned;
    private boolean finished;

    public UnassignedMatch() {}
    public UnassignedMatch(UUID matchId, UUID competitionId) {
        this.id = new UnassignedMatchId(matchId, competitionId);
        this.assigned = false;
        this.finished = false;
    }

    public UnassignedMatchId getId() {
        return id;
    }

    public void setId(UnassignedMatchId id) {
        this.id = id;
    }

    public boolean isAssigned() {
        return assigned;
    }

    public void setAssigned(boolean assigned) {
        this.assigned = assigned;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}
