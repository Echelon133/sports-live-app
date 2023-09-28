package ml.echelon133.matchservice.match;

import ml.echelon133.common.match.MatchResult;
import ml.echelon133.common.match.MatchStatus;
import ml.echelon133.common.match.dto.MatchDto;
import ml.echelon133.common.referee.dto.RefereeDto;
import ml.echelon133.common.venue.dto.VenueDto;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TestMatchDto {
    static MatchDto.MatchDtoBuilder builder() {
        // use the existing builder to create a MatchDto with some default test values
        return MatchDto.builder()
                .id(UUID.randomUUID())
                .status(MatchStatus.NOT_STARTED.toString())
                .result(MatchResult.NONE.toString())
                .competitionId(UUID.randomUUID())
                .startTimeUTC(LocalDateTime.of(2023, 1, 1, 20, 0))
                .homeTeam(
                        MatchDto.ShortTeamDto.from(UUID.randomUUID(), "Test Team A", "")
                )
                .awayTeam(
                        MatchDto.ShortTeamDto.from(UUID.randomUUID(), "Test Team B", "")
                )
                .venue(VenueDto.from(UUID.randomUUID(), "Test Venue", 1000))
                .referee(RefereeDto.from(UUID.randomUUID(), "Test Referee"))
                .halfTimeScoreInfo(MatchDto.ScoreInfoDto.from((byte)0, (byte)0))
                .scoreInfo(MatchDto.ScoreInfoDto.from((byte)0, (byte)0))
                .penaltiesInfo(MatchDto.PenaltiesInfoDto.from((byte)0, (byte)0));
    }
}
