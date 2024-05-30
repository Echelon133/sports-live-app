package ml.echelon133.matchservice.match.model;

import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CompactMatchDto {
    UUID getId();
    String getStatus();
    String getResult();
    UUID getCompetitionId();
    LocalDateTime getStartTimeUTC();

    // if homeTeam is deleted, set this value to null to prevent any leakage of data
    @Value("#{target.homeTeamDeleted ? null : (T(ml.echelon133.matchservice.match.model.ShortTeamDto).from(target.homeTeamId, target.homeTeamName, target.homeTeamCrestUrl))}")
    ShortTeamDto getHomeTeam();

    // if awayTeam is deleted, set this value to null to prevent any leakage of data
    @Value("#{target.awayTeamDeleted ? null : (T(ml.echelon133.matchservice.match.model.ShortTeamDto).from(target.awayTeamId, target.awayTeamName, target.awayTeamCrestUrl))}")
    ShortTeamDto getAwayTeam();

    @Value("#{T(ml.echelon133.matchservice.match.model.ScoreInfoDto).from(target.halfTimeHomeGoals, target.halfTimeAwayGoals)}")
    ScoreInfoDto getHalfTimeScoreInfo();

    @Value("#{T(ml.echelon133.matchservice.match.model.ScoreInfoDto).from(target.homeGoals, target.awayGoals)}")
    ScoreInfoDto getScoreInfo();

    @Value("#{T(ml.echelon133.matchservice.match.model.ScoreInfoDto).from(target.homePenalties, target.awayPenalties)}")
    ScoreInfoDto getPenaltiesInfo();


    static CompactMatchDtoBuilder builder() {
        return new CompactMatchDtoBuilder();
    }

    class CompactMatchDtoBuilder {
        private UUID id;
        private String status;
        private String result;
        private UUID competitionId;
        private LocalDateTime startTimeUTC;
        private ShortTeamDto homeTeam;
        private ShortTeamDto awayTeam;
        private ScoreInfoDto halfTimeScoreInfo;
        private ScoreInfoDto scoreInfo;
        private ScoreInfoDto penaltiesInfo;

        private CompactMatchDtoBuilder() {}

        public CompactMatchDtoBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public CompactMatchDtoBuilder status(String status) {
            this.status = status;
            return this;
        }

        public CompactMatchDtoBuilder result(String result) {
            this.result = result;
            return this;
        }

        public CompactMatchDtoBuilder competitionId(UUID competitionId) {
            this.competitionId = competitionId;
            return this;
        }

        public CompactMatchDtoBuilder startTimeUTC(LocalDateTime startTimeUTC) {
            this.startTimeUTC = startTimeUTC;
            return this;
        }

        public CompactMatchDtoBuilder homeTeam(ShortTeamDto homeTeam) {
            this.homeTeam = homeTeam;
            return this;
        }

        public CompactMatchDtoBuilder awayTeam(ShortTeamDto awayTeam) {
            this.awayTeam = awayTeam;
            return this;
        }

        public CompactMatchDtoBuilder halfTimeScoreInfo(ScoreInfoDto halfTimeScoreInfo) {
            this.halfTimeScoreInfo = halfTimeScoreInfo;
            return this;
        }

        public CompactMatchDtoBuilder scoreInfo(ScoreInfoDto scoreInfo) {
            this.scoreInfo = scoreInfo;
            return this;
        }

        public CompactMatchDtoBuilder penaltiesInfo(ScoreInfoDto penaltiesInfo) {
            this.penaltiesInfo = penaltiesInfo;
            return this;
        }

        public CompactMatchDto build() {
            return new CompactMatchDto() {
                @Override
                public UUID getId() {
                    return id;
                }

                @Override
                public String getStatus() {
                    return status;
                }

                @Override
                public UUID getCompetitionId() {
                    return competitionId;
                }

                @Override
                public String getResult() {
                    return result;
                }

                @Override
                public LocalDateTime getStartTimeUTC() {
                    return startTimeUTC;
                }

                @Override
                public ShortTeamDto getHomeTeam() {
                    return homeTeam;
                }

                @Override
                public ShortTeamDto getAwayTeam() {
                    return awayTeam;
                }

                @Override
                public ScoreInfoDto getHalfTimeScoreInfo() {
                    return halfTimeScoreInfo;
                }

                @Override
                public ScoreInfoDto getScoreInfo() {
                    return scoreInfo;
                }

                @Override
                public ScoreInfoDto getPenaltiesInfo() {
                    return penaltiesInfo;
                }
            };
        }
    }
}
