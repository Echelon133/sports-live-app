package ml.echelon133.matchservice.match;

import ml.echelon133.common.match.MatchResult;
import ml.echelon133.common.match.MatchStatus;
import ml.echelon133.matchservice.match.model.MatchDto;
import ml.echelon133.matchservice.match.model.ScoreInfoDto;
import ml.echelon133.matchservice.match.model.ShortTeamDto;
import ml.echelon133.matchservice.referee.model.RefereeDto;
import ml.echelon133.matchservice.venue.model.VenueDto;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TestMatchDto {
    static MatchDto.MatchDtoBuilder builder() {
        // use the existing builder to create a MatchDto with some default test values
        return MatchDto.builder()
                .id(UUID.randomUUID())
                .status(MatchStatus.NOT_STARTED.toString())
                .statusLastModifiedUTC(null)
                .result(MatchResult.NONE.toString())
                .competitionId(UUID.randomUUID())
                .startTimeUTC(LocalDateTime.of(2023, 1, 1, 20, 0))
                .homeTeam(
                        new ShortTeamDto(UUID.randomUUID(), "Test Team A", "")
                )
                .awayTeam(
                        new ShortTeamDto(UUID.randomUUID(), "Test Team B", "")
                )
                .venue(VenueDto.from(UUID.randomUUID(), "Test Venue", 1000))
                .referee(RefereeDto.from(UUID.randomUUID(), "Test Referee"))
                .halfTimeScoreInfo(new ScoreInfoDto(0, 0))
                .scoreInfo(new ScoreInfoDto(0, 0))
                .penaltiesInfo(new ScoreInfoDto(0, 0));
    }
}
