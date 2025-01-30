package pl.echelon133.competitionservice.competition.model;

import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class CompetitionMatch {

    private UUID matchId;
    private boolean finished;

    public CompetitionMatch() {
    }
    public CompetitionMatch(UUID matchId) {
        this.matchId = matchId;
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
