package ml.echelon133.matchservice.match.model;

import ml.echelon133.matchservice.match.model.constraints.FormationCorrect;
import ml.echelon133.matchservice.match.model.constraints.PlayerIdsUnique;
import ml.echelon133.matchservice.team.constraints.TeamPlayerExists;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@PlayerIdsUnique
public class UpsertLineupDto {

    @NotNull
    @Size(min = 11, max = 11, message = "starting lineup requires exactly 11 players")
    private List<@TeamPlayerExists String> startingPlayers;

    @NotNull
    private List<@TeamPlayerExists String> substitutePlayers;

    @FormationCorrect
    private String formation;

    public UpsertLineupDto() {}
    public UpsertLineupDto(List<String> startingPlayers, List<String> substitutePlayers) {
        this.startingPlayers = startingPlayers;
        this.substitutePlayers = substitutePlayers;
    }
    public UpsertLineupDto(List<String> startingPlayers, List<String> substitutePlayers, String formation) {
        this(startingPlayers, substitutePlayers);
        this.formation = formation;
    }

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

    public String getFormation() {
        return formation;
    }

    public void setFormation(String formation) {
        this.formation = formation;
    }
}
