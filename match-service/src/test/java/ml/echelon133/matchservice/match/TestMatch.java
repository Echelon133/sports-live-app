package ml.echelon133.matchservice.match;

import ml.echelon133.common.match.MatchResult;
import ml.echelon133.common.match.MatchStatus;
import ml.echelon133.matchservice.match.model.Lineup;
import ml.echelon133.matchservice.match.model.Match;
import ml.echelon133.matchservice.match.model.ScoreInfo;
import ml.echelon133.matchservice.referee.model.Referee;
import ml.echelon133.matchservice.team.TestTeam;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.venue.model.Venue;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TestMatch {
    static MatchBuilder builder() {
        return new MatchBuilder();
    }

    class MatchBuilder {
        private UUID id = UUID.randomUUID();
        private MatchStatus status = MatchStatus.NOT_STARTED;
        private Team homeTeam = TestTeam.builder().name("Team A").build();
        private Team awayTeam = TestTeam.builder().name("Team B").build();
        private LocalDateTime startTimeUTC = LocalDateTime.now();
        private Venue venue = new Venue("Test Venue", 50000);
        private Referee referee = new Referee("Test Referee");
        private UUID competitionId = UUID.randomUUID();
        private ScoreInfo halfTimeScoreInfo = new ScoreInfo();
        private ScoreInfo scoreInfo = new ScoreInfo();
        private ScoreInfo penaltiesInfo = new ScoreInfo();
        private MatchResult result = MatchResult.NONE;
        private boolean deleted = false;

        private MatchBuilder() {}

        public MatchBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public MatchBuilder status(MatchStatus status) {
            this.status = status;
            return this;
        }

        public MatchBuilder homeTeam(TestTeam.TeamBuilder homeTeamBuilder) {
            this.homeTeam = homeTeamBuilder.build();
            return this;
        }

        public MatchBuilder homeTeam(Team homeTeam) {
            this.homeTeam = homeTeam;
            return this;
        }

        public MatchBuilder awayTeam(TestTeam.TeamBuilder awayTeamBuilder) {
            this.awayTeam = awayTeamBuilder.build();
            return this;
        }

        public MatchBuilder awayTeam(Team awayTeam) {
            this.awayTeam = awayTeam;
            return this;
        }

        public MatchBuilder startTimeUTC(LocalDateTime startTimeUTC) {
            this.startTimeUTC = startTimeUTC;
            return this;
        }

        public MatchBuilder venue(Venue venue) {
            this.venue = venue;
            return this;
        }

        public MatchBuilder referee(Referee referee) {
            this.referee = referee;
            return this;
        }

        public MatchBuilder competitionId(UUID competitionId) {
            this.competitionId = competitionId;
            return this;
        }

        public MatchBuilder halfTimeScoreInfo(ScoreInfo halfTimeScoreInfo) {
            this.halfTimeScoreInfo = halfTimeScoreInfo;
            return this;
        }

        public MatchBuilder scoreInfo(ScoreInfo scoreInfo) {
            this.scoreInfo = scoreInfo;
            return this;
        }

        public MatchBuilder penaltiesInfo(ScoreInfo penaltiesInfo) {
            this.penaltiesInfo = penaltiesInfo;
            return this;
        }

        public MatchBuilder result(MatchResult result) {
            this.result = result;
            return this;
        }

        public MatchBuilder deleted(boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        public Match build() {
            var match = new Match();
            match.setId(id);
            match.setStatus(status);
            match.setHomeTeam(homeTeam);
            match.setAwayTeam(awayTeam);
            match.setStartTimeUTC(startTimeUTC);
            match.setVenue(venue);
            match.setReferee(referee);
            match.setCompetitionId(competitionId);
            match.setHalfTimeScoreInfo(halfTimeScoreInfo);
            match.setScoreInfo(scoreInfo);
            match.setPenaltiesInfo(penaltiesInfo);
            match.setResult(result);
            match.setDeleted(deleted);
            match.setHomeLineup(new Lineup());
            match.setAwayLineup(new Lineup());
            return match;
        }
    }
}
