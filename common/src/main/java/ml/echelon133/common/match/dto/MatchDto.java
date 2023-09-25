package ml.echelon133.common.match.dto;

import ml.echelon133.common.referee.dto.RefereeDto;
import ml.echelon133.common.venue.dto.VenueDto;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.UUID;

public interface MatchDto {
    UUID getId();
    String getStatus();
    String getResult();
    UUID getCompetitionId();
    LocalDateTime getStartTimeUTC();

    // if homeTeam is deleted, set this value to null to prevent any leakage of data
    @Value("#{target.homeTeamDeleted ? null : (T(ml.echelon133.common.match.dto.MatchDto.ShortTeamDto).from(target.homeTeamId, target.homeTeamName, target.homeTeamCrestUrl))}")
    ShortTeamDto getHomeTeam();

    // if awayTeam is deleted, set this value to null to prevent any leakage of data
    @Value("#{target.awayTeamDeleted ? null : (T(ml.echelon133.common.match.dto.MatchDto.ShortTeamDto).from(target.awayTeamId, target.awayTeamName, target.awayTeamCrestUrl))}")
    ShortTeamDto getAwayTeam();

    // if venue is deleted, set this value to null to prevent any leakage of data
    @Value("#{target.venueDeleted ? null : (T(ml.echelon133.common.venue.dto.VenueDto).from(target.venueId, target.venueName, target.venueCapacity))}")
    VenueDto getVenue();

    // referee is optional - if refereeDeleted is null or true, then its content should not be displayed
    @Value("#{(target.refereeDeleted == null || target.refereeDeleted == true) ? null : (T(ml.echelon133.common.referee.dto.RefereeDto).from(target.refereeId, target.refereeName))}")
    RefereeDto getReferee();

    @Value("#{T(ml.echelon133.common.match.dto.MatchDto.ScoreInfoDto).from(target.homeGoals, target.awayGoals)}")
    ScoreInfoDto getScoreInfo();

    @Value("#{T(ml.echelon133.common.match.dto.MatchDto.PenaltiesInfoDto).from(target.homePenalties, target.awayPenalties)}")
    PenaltiesInfoDto getPenaltiesInfo();


    interface ShortTeamDto {
        UUID getId();
        String getName();
        String getCrestUrl();

        static ShortTeamDto from(UUID teamId, String teamName, String teamCrestUrl) {
            return new ShortTeamDto() {
                @Override
                public UUID getId() {
                    return teamId;
                }

                @Override
                public String getName() {
                    return teamName;
                }

                @Override
                public String getCrestUrl() {
                    return teamCrestUrl;
                }
            };
        }
    }

    interface ScoreInfoDto {
        Integer getHomeGoals();
        Integer getAwayGoals();

        static ScoreInfoDto from(Integer homeGoals, Integer awayGoals) {
            return new ScoreInfoDto() {
                @Override
                public Integer getHomeGoals() {
                    return homeGoals;
                }

                @Override
                public Integer getAwayGoals() {
                    return awayGoals;
                }
            };
        }
    }

    interface PenaltiesInfoDto {
        Integer getHomePenalties();
        Integer getAwayPenalties();

        static PenaltiesInfoDto from(Integer homePenalties, Integer awayPenalties) {
            return new PenaltiesInfoDto() {
                @Override
                public Integer getHomePenalties() {
                    return homePenalties;
                }

                @Override
                public Integer getAwayPenalties() {
                    return awayPenalties;
                }
            };
        }
    }
}
