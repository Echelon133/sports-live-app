package ml.echelon133.matchservice.match.service;

import ml.echelon133.common.match.dto.MatchDto;
import ml.echelon133.common.match.dto.PenaltiesInfoDto;
import ml.echelon133.common.match.dto.ScoreInfoDto;
import ml.echelon133.common.match.dto.ShortTeamDto;
import ml.echelon133.common.referee.dto.RefereeDto;
import ml.echelon133.common.venue.dto.VenueDto;
import ml.echelon133.matchservice.match.model.Match;

public class MatchMapper {

    public static MatchDto entityToDto(Match match) {
        var homeTeam = match.getHomeTeam();
        var awayTeam = match.getAwayTeam();
        var venue = match.getVenue();
        var referee = match.getReferee();
        var halfTimeScoreInfo = match.getHalfTimeScoreInfo();
        var scoreInfo = match.getScoreInfo();
        var penaltiesInfo = match.getPenaltiesInfo();

        ShortTeamDto homeTeamDto = null;
        // only set homeTeam if the entity is not marked as deleted
        if (!homeTeam.isDeleted()) {
            homeTeamDto = ShortTeamDto.from(
                    homeTeam.getId(),
                    homeTeam.getName(),
                    homeTeam.getCrestUrl()
            );
        }

        ShortTeamDto awayTeamDto = null;
        // only set awayTeam if the entity is not marked as deleted
        if (!awayTeam.isDeleted()) {
            awayTeamDto = ShortTeamDto.from(
                    awayTeam.getId(),
                    awayTeam.getName(),
                    awayTeam.getCrestUrl()
            );
        }

        VenueDto venueDto = null;
        // only set venue if the entity is not marked as deleted
        if (!venue.isDeleted()) {
            venueDto = VenueDto.from(
                    venue.getId(),
                    venue.getName(),
                    venue.getCapacity()
            );
        }

        RefereeDto refereeDto = null;
        // referee is optional, only set it if the entity is not null and not marked as deleted
        if (referee != null && !referee.isDeleted()) {
            refereeDto = RefereeDto.from(
                    referee.getId(),
                    referee.getName()
            );
        }

        ScoreInfoDto halfTimeScoreInfoDto = ScoreInfoDto.from(
                halfTimeScoreInfo.getHomeGoals(),
                halfTimeScoreInfo.getAwayGoals()
        );
        ScoreInfoDto scoreInfoDto = ScoreInfoDto.from(
                scoreInfo.getHomeGoals(),
                scoreInfo.getAwayGoals()
        );
        PenaltiesInfoDto penaltiesInfoDto = PenaltiesInfoDto.from(
                penaltiesInfo.getHomePenalties(),
                penaltiesInfo.getAwayPenalties()
        );

        return MatchDto.builder()
                .id(match.getId())
                .status(match.getStatus().toString())
                .result(match.getResult().toString())
                .competitionId(match.getCompetitionId())
                .startTimeUTC(match.getStartTimeUTC())
                .homeTeam(homeTeamDto)
                .awayTeam(awayTeamDto)
                .venue(venueDto)
                .referee(refereeDto)
                .halfTimeScoreInfo(halfTimeScoreInfoDto)
                .scoreInfo(scoreInfoDto)
                .penaltiesInfo(penaltiesInfoDto)
                .build();
    }
}
