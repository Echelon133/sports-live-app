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
}
