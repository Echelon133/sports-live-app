package pl.echelon133.competitionservice.competition.model;

import jakarta.persistence.Entity;
import ml.echelon133.common.entity.BaseEntity;

import java.util.UUID;

@Entity
public class UnassignedMatch extends BaseEntity {

    private UUID matchId;
    private UUID competitionId;

    public UnassignedMatch() {}
    public UnassignedMatch(UUID matchId, UUID competitionId) {
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
