package pl.echelon133.competitionservice.competition.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import ml.echelon133.common.entity.BaseEntity;

import java.util.UUID;

@Entity
public class LeagueSlot extends BaseEntity {

    @Embedded
    private CompetitionMatch match;
    private UUID competitionId;
    private int round;

    public LeagueSlot() {}

    public LeagueSlot(CompetitionMatch match, UUID competitionId, int round) {
        this.match = match;
        this.competitionId = competitionId;
        this.round = round;
    }

    public CompetitionMatch getMatch() {
        return match;
    }

    public void setMatch(CompetitionMatch match) {
        this.match = match;
    }

    public UUID getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(UUID competitionId) {
        this.competitionId = competitionId;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }
}
