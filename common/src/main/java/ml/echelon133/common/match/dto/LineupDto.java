package ml.echelon133.common.match.dto;

import ml.echelon133.common.team.dto.TeamPlayerDto;

import java.util.List;
import java.util.Map;

public class LineupDto {

    private static final String STARTING_PLAYERS = "startingPlayers";
    private static final String SUBSTITUTE_PLAYERS = "substitutePlayers";

    private final Map<String, List<TeamPlayerDto>> home;
    private final Map<String, List<TeamPlayerDto>> away;

    public LineupDto() {
        this.home = Map.of(STARTING_PLAYERS, List.of(), SUBSTITUTE_PLAYERS, List.of());
        this.away = Map.of(STARTING_PLAYERS, List.of(), SUBSTITUTE_PLAYERS, List.of());
    }
    public LineupDto(
            List<TeamPlayerDto> homeStartingPlayers,
            List<TeamPlayerDto> homeSubstitutePlayers,
            List<TeamPlayerDto> awayStartingPlayers,
            List<TeamPlayerDto> awaySubstitutePlayers
    ) {
        this.home = Map.of(
                STARTING_PLAYERS, homeStartingPlayers,
                SUBSTITUTE_PLAYERS, homeSubstitutePlayers
        );
        this.away = Map.of(
                STARTING_PLAYERS, awayStartingPlayers,
                SUBSTITUTE_PLAYERS, awaySubstitutePlayers
        );
    }

    public Map<String, List<TeamPlayerDto>> getHome() {
        return home;
    }

    public Map<String, List<TeamPlayerDto>> getAway() {
        return away;
    }
}
