package ml.echelon133.matchservice.match.model;

import ml.echelon133.matchservice.match.model.constraints.FormationCorrect;
import ml.echelon133.matchservice.match.model.constraints.PlayerIdsUnique;
import ml.echelon133.matchservice.team.constraints.TeamPlayerExists;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@PlayerIdsUnique
public record UpsertLineupDto(
    @NotNull @Size(min = 11, max = 11, message = "starting lineup requires exactly 11 players") List<@TeamPlayerExists String> startingPlayers,
    @NotNull List<@TeamPlayerExists String> substitutePlayers,
    @FormationCorrect String formation
) {}