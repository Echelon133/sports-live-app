package ml.echelon133.common.team.dto;

import ml.echelon133.common.match.MatchResult;
import ml.echelon133.common.match.dto.ScoreInfoDto;
import ml.echelon133.common.match.dto.ShortTeamDto;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TeamFormDetailsDto {
    UUID getId();
    MatchResult getResult();
    LocalDateTime getStartTimeUTC();

    // if homeTeam is deleted, set this value to null to prevent any leakage of data
    @Value("#{target.homeTeamDeleted ? null : (T(ml.echelon133.common.match.dto.ShortTeamDto).from(target.homeTeamId, target.homeTeamName, target.homeTeamCrestUrl))}")
    ShortTeamDto getHomeTeam();

    // if awayTeam is deleted, set this value to null to prevent any leakage of data
    @Value("#{target.awayTeamDeleted ? null : (T(ml.echelon133.common.match.dto.ShortTeamDto).from(target.awayTeamId, target.awayTeamName, target.awayTeamCrestUrl))}")
    ShortTeamDto getAwayTeam();

    @Value("#{T(ml.echelon133.common.match.dto.ScoreInfoDto).from(target.homeGoals, target.awayGoals)}")
    ScoreInfoDto getScoreInfo();

    static TeamFormDetailsDto from(
            UUID id, LocalDateTime start, ShortTeamDto home, ShortTeamDto away, ScoreInfoDto score
    ) {
        return new TeamFormDetailsDto() {
            @Override
            public UUID getId() {
                return id;
            }

            @Override
            public MatchResult getResult() {
                return MatchResult.getResultBasedOnScore(score.getHomeGoals(), score.getAwayGoals());
            }

            @Override
            public LocalDateTime getStartTimeUTC() {
                return start;
            }

            @Override
            public ShortTeamDto getHomeTeam() {
                return home;
            }

            @Override
            public ShortTeamDto getAwayTeam() {
                return away;
            }

            @Override
            public ScoreInfoDto getScoreInfo() {
                return score;
            }
        };
    }
}
