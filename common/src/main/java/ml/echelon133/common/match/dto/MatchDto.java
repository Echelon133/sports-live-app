package ml.echelon133.common.match.dto;

import ml.echelon133.common.referee.dto.RefereeDto;
import ml.echelon133.common.venue.dto.VenueDto;

import java.time.LocalDateTime;
import java.util.UUID;

public interface MatchDto {
    UUID getId();
    String getStatus();
    String getResult();
    UUID getCompetitionId();
    LocalDateTime getStartTimeUTC();

    ShortTeamDto getHomeTeam();
    ShortTeamDto getAwayTeam();
    VenueDto getVenue();
    RefereeDto getReferee();
    ScoreInfoDto getScoreInfo();
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
