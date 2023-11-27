package ml.echelon133.matchservice.match.model;

import ml.echelon133.common.entity.BaseEntity;
import ml.echelon133.matchservice.team.model.TeamPlayer;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Lineup extends BaseEntity {

    @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinTable(
            name = "starting_player",
            joinColumns = @JoinColumn(name = "lineup_id"),
            inverseJoinColumns = @JoinColumn(name = "team_player_id")
    )
    List<TeamPlayer> startingPlayers;

    @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinTable(
            name = "substitute_player",
            joinColumns = @JoinColumn(name = "lineup_id"),
            inverseJoinColumns = @JoinColumn(name = "team_player_id")
    )
    List<TeamPlayer> substitutePlayers;

    public Lineup() {
        this.startingPlayers = new ArrayList<>();
        this.substitutePlayers = new ArrayList<>();
    }
    public Lineup(List<TeamPlayer> startingPlayers, List<TeamPlayer> substitutePlayers) {
        this.startingPlayers = startingPlayers;
        this.substitutePlayers = substitutePlayers;
    }

    public List<TeamPlayer> getStartingPlayers() {
        return startingPlayers;
    }

    public void setStartingPlayers(List<TeamPlayer> startingPlayers) {
        this.startingPlayers = startingPlayers;
    }

    public List<TeamPlayer> getSubstitutePlayers() {
        return substitutePlayers;
    }

    public void setSubstitutePlayers(List<TeamPlayer> substitutePlayers) {
        this.substitutePlayers = substitutePlayers;
    }
}
