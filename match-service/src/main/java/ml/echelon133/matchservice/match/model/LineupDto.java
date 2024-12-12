package ml.echelon133.matchservice.match.model;

import ml.echelon133.matchservice.team.model.TeamPlayerDto;

import java.util.List;

public record LineupDto(TeamLineup home, TeamLineup away) {
    public record TeamLineup(
        List<TeamPlayerDto> startingPlayers,
        List<TeamPlayerDto> substitutePlayers,
        String formation
    ) {}
}