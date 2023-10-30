package ml.echelon133.matchservice.team.model;

import ml.echelon133.matchservice.player.constraints.PlayerExists;
import ml.echelon133.matchservice.player.model.constraints.PositionValue;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;

public class UpsertTeamPlayerDto {

    @NotNull
    @PlayerExists
    private String playerId;

    @NotNull
    @PositionValue
    private String position;

    @NotNull
    @Range(min = 1, max = 99, message = "expected number between 1 and 99")
    private Integer number;

    public UpsertTeamPlayerDto() {}
    public UpsertTeamPlayerDto(String playerId, String position, Integer number) {
        this.playerId = playerId;
        this.position = position;
        this.number = number;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }
}
