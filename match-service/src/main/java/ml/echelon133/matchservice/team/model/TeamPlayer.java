package ml.echelon133.matchservice.team.model;

import ml.echelon133.common.entity.BaseEntity;
import ml.echelon133.matchservice.player.model.Player;
import ml.echelon133.matchservice.player.model.Position;

import jakarta.persistence.*;

@Entity
@Table(
        name = "team_player",
        indexes = {
                @Index(columnList = "team_id", name = "team_id_index"),
                @Index(columnList = "player_id", name = "player_id_index"),
        }
)
public class TeamPlayer extends BaseEntity {

    @ManyToOne(optional = false, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(optional = false, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Position position;

    @Column(nullable = false)
    private Integer number;

    public TeamPlayer() {}
    public TeamPlayer(Team team, Player player, Position position, Integer number) {
        this.team = team;
        this.player = player;
        this.position = position;
        this.number = number;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }
}
