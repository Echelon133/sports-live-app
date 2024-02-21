package pl.echelon133.competitionservice.competition.model;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class StandingsDto {

    private List<GroupDto> groups;
    private List<LegendDto> legend;

    public StandingsDto() {}
    public StandingsDto(List<GroupDto> groups, List<LegendDto> legend) {
        this.groups = groups;
        this.legend = legend;
    }

    public List<GroupDto> getGroups() {
        return groups;
    }

    public List<LegendDto> getLegend() {
        return legend;
    }

    public static class GroupDto {
        private String name;
        private List<TeamStatsDto> teams;

        public GroupDto() {}
        public GroupDto(String name, List<TeamStatsDto> teamStats) {
            this.name = name;
            this.teams = teamStats;
        }

        public String getName() {
            return name;
        }

        public List<TeamStatsDto> getTeams() {
            return teams;
        }
    }

    public static class LegendDto {
        private Set<Integer> positions;
        private String context;
        private String sentiment;

        public LegendDto() {}
        public LegendDto(Set<Integer> positions, String context, String sentiment) {
            this.positions = positions;
            this.context = context;
            this.sentiment = sentiment;
        }
        public LegendDto(Legend legend) {
            this(legend.getPositions(), legend.getContext(), legend.getSentiment().toString());
        }

        public Set<Integer> getPositions() {
            return positions;
        }

        public String getContext() {
            return context;
        }

        public String getSentiment() {
            return sentiment;
        }
    }

    public static class TeamStatsDto {
        private UUID teamId;
        private String teamName;
        private String crestUrl;
        private int matchesPlayed;
        private int wins;
        private int draws;
        private int losses;
        private int goalsScored;
        private int goalsConceded;
        private int points;

        public TeamStatsDto() {}
        public TeamStatsDto(UUID teamId, String teamName, String crestUrl) {
            this.teamId = teamId;
            this.teamName = teamName;
            this.crestUrl = crestUrl;
        }
        public TeamStatsDto(TeamStats stats) {
            this(stats.getTeamId(), stats.getTeamName(), stats.getCrestUrl());
            this.matchesPlayed = stats.getMatchesPlayed();
            this.wins = stats.getWins();
            this.draws = stats.getDraws();
            this.losses = stats.getLosses();
            this.goalsScored = stats.getGoalsScored();
            this.goalsConceded = stats.getGoalsConceded();
            this.points = stats.getPoints();
        }

        public UUID getTeamId() {
            return teamId;
        }

        public String getTeamName() {
            return teamName;
        }

        public String getCrestUrl() {
            return crestUrl;
        }

        public int getMatchesPlayed() {
            return matchesPlayed;
        }

        public int getWins() {
            return wins;
        }

        public int getDraws() {
            return draws;
        }

        public int getLosses() {
            return losses;
        }

        public int getGoalsScored() {
            return goalsScored;
        }

        public int getGoalsConceded() {
            return goalsConceded;
        }

        public int getPoints() {
            return points;
        }
    }
}
