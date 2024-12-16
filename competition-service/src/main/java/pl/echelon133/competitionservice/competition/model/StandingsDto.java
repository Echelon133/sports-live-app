package pl.echelon133.competitionservice.competition.model;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record StandingsDto(List<GroupDto> groups, List<LegendDto> legend) {

    public record GroupDto(String name, List<TeamStatsDto> teams) {
    }

    public record LegendDto(Set<Integer> positions, String context, String sentiment) {
        public LegendDto(Legend legend) {
            this(legend.getPositions(), legend.getContext(), legend.getSentiment().toString());
        }
    }

    public record TeamStatsDto(
            UUID teamId, String teamName, String crestUrl, int matchesPlayed,
            int wins, int draws, int losses, int goalsScored,
            int goalsConceded, int points
    ) {
        public TeamStatsDto(UUID teamId, String teamName, String crestUrl) {
            this(teamId, teamName, crestUrl, 0, 0, 0, 0, 0, 0, 0);
        }

        public TeamStatsDto(TeamStats stats) {
            this(
                    stats.getTeamId(), stats.getTeamName(), stats.getCrestUrl(), stats.getMatchesPlayed(),
                    stats.getWins(), stats.getDraws(), stats.getLosses(), stats.getGoalsScored(),
                    stats.getGoalsConceded(), stats.getPoints()
            );
        }
    }
}
