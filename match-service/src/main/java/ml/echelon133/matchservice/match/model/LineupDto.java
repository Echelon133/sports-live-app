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
    public LineupDto(
            List<TeamPlayerDto> homeStartingPlayers,
            List<TeamPlayerDto> homeSubstitutePlayers,
            List<TeamPlayerDto> awayStartingPlayers,
            List<TeamPlayerDto> awaySubstitutePlayers,
            LineupFormationsDto lineupFormations
    ) {
        this.home = new TeamLineup(
                homeStartingPlayers, homeSubstitutePlayers, lineupFormations.getHomeFormation()
        );
        this.away = new TeamLineup(
                awayStartingPlayers, awaySubstitutePlayers, lineupFormations.getAwayFormation()
        );
    }

    public static class TeamLineup {
        private final List<TeamPlayerDto> startingPlayers;
        private final List<TeamPlayerDto> substitutePlayers;
        private String formation;

        public TeamLineup() {
            this.startingPlayers = List.of();
            this.substitutePlayers = List.of();
        }
        public TeamLineup(List<TeamPlayerDto> startingPlayers, List<TeamPlayerDto> substitutePlayers) {
            this.startingPlayers = startingPlayers;
            this.substitutePlayers = substitutePlayers;
        }
        public TeamLineup(
                List<TeamPlayerDto> startingPlayers,
                List<TeamPlayerDto> substitutePlayers,
                String formation
        ) {
            this(startingPlayers, substitutePlayers);
            this.formation = formation;
        }

        public List<TeamPlayerDto> getStartingPlayers() {
            return startingPlayers;
        }

        public List<TeamPlayerDto> getSubstitutePlayers() {
            return substitutePlayers;
        }

        public String getFormation() {
            return formation;
        }
    }

    public TeamLineup getHome() {
        return home;
    }

    public TeamLineup getAway() {
        return away;
    }
}
