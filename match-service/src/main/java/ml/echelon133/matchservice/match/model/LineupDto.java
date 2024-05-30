package ml.echelon133.matchservice.match.model;

import ml.echelon133.matchservice.team.model.TeamPlayerDto;

import java.util.List;

public class LineupDto {

    private final TeamLineup home;
    private final TeamLineup away;

    public LineupDto() {
        this.home = new TeamLineup();
        this.away = new TeamLineup();
    }
    public LineupDto(
            List<TeamPlayerDto> homeStartingPlayers,
            List<TeamPlayerDto> homeSubstitutePlayers,
            List<TeamPlayerDto> awayStartingPlayers,
            List<TeamPlayerDto> awaySubstitutePlayers
    ) {
        this.home = new TeamLineup(homeStartingPlayers, homeSubstitutePlayers);
        this.away = new TeamLineup(awayStartingPlayers, awaySubstitutePlayers);
    }

    public static class TeamLineup {
        private final List<TeamPlayerDto> startingPlayers;
        private final List<TeamPlayerDto> substitutePlayers;

        public TeamLineup() {
            this.startingPlayers = List.of();
            this.substitutePlayers = List.of();
        }
        public TeamLineup(List<TeamPlayerDto> startingPlayers, List<TeamPlayerDto> substitutePlayers) {
            this.startingPlayers = startingPlayers;
            this.substitutePlayers = substitutePlayers;
        }

        public List<TeamPlayerDto> getStartingPlayers() {
            return startingPlayers;
        }

        public List<TeamPlayerDto> getSubstitutePlayers() {
            return substitutePlayers;
        }
    }

    public TeamLineup getHome() {
        return home;
    }

    public TeamLineup getAway() {
        return away;
    }
}
