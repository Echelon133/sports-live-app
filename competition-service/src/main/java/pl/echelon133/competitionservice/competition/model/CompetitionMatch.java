package pl.echelon133.competitionservice.competition.model;

import jakarta.persistence.Entity;
import ml.echelon133.common.entity.BaseEntity;

import java.util.UUID;

@Entity
public class CompetitionMatch extends BaseEntity {

    private UUID matchId;
    private boolean finished;

    public CompetitionMatch() {
    }
    public CompetitionMatch(UUID matchId) {
        this.matchId = matchId;
    }
    public CompetitionMatch(UUID matchId, boolean finished) {
        this(matchId);
        this.finished = finished;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}
