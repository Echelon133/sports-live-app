package pl.echelon133.competitionservice.competition.model;

import ml.echelon133.common.entity.BaseEntity;

import javax.persistence.*;
import java.util.UUID;

@Entity
public class PlayerStats extends BaseEntity {

    @Column(nullable = false)
    private UUID playerId;

    @Column(nullable = false)
    private UUID teamId;

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id")
    private Competition competition;

    private int goals;
    private int assists;
    private int yellowCards;
    private int redCards;

    public PlayerStats() {}
    public PlayerStats(UUID playerId, UUID teamId, String name) {
        this.playerId = playerId;
        this.teamId = teamId;
        this.name = name;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGoals() {
        return goals;
    }

    public void setGoals(int goals) {
        this.goals = goals;
    }

    public int getAssists() {
        return assists;
    }

    public void setAssists(int assists) {
        this.assists = assists;
    }

    public int getYellowCards() {
        return yellowCards;
    }

    public void setYellowCards(int yellowCards) {
        this.yellowCards = yellowCards;
    }

    public int getRedCards() {
        return redCards;
    }

    public void setRedCards(int redCards) {
        this.redCards = redCards;
    }

    public Competition getCompetition() {
        return competition;
    }

    public void setCompetition(Competition competition) {
        this.competition = competition;
    }

    public void incrementGoals() {
        this.goals += 1;
    }

    public void incrementAssists() {
        this.assists += 1;
    }

    public void incrementYellowCards() {
        this.yellowCards += 1;
    }

    public void incrementRedCards() {
        this.redCards += 1;
    }
}
