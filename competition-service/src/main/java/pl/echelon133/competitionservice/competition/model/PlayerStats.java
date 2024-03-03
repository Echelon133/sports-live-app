package pl.echelon133.competitionservice.competition.model;

import ml.echelon133.common.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.UUID;

@Entity
public class PlayerStats extends BaseEntity {

    @Column(nullable = false)
    private UUID playerId;

    @Column(nullable = false)
    private UUID teamId;

    @Column(nullable = false, length = 200)
    private String name;

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
}
