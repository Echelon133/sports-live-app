package ml.echelon133.matchservice.match.model;

import ml.echelon133.matchservice.match.model.constraints.PlayerIdsUnique;
import ml.echelon133.matchservice.team.constraints.TeamPlayerExists;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@PlayerIdsUnique
public class UpsertLineupDto {

    @NotNull
    @Size(min = 11, max = 11, message = "starting lineup requires exactly 11 players")
    private List<@TeamPlayerExists String> startingPlayers;

    @NotNull
    private List<@TeamPlayerExists String> substitutePlayers;

    public UpsertLineupDto() {}

    public List<String> getStartingPlayers() {
        return startingPlayers;
    }

    public void setStartingPlayers(List<String> startingPlayers) {
        this.startingPlayers = startingPlayers;
    }

    public List<String> getSubstitutePlayers() {
        return substitutePlayers;
    }

    public void setSubstitutePlayers(List<String> substitutePlayers) {
        this.substitutePlayers = substitutePlayers;
    }
}
