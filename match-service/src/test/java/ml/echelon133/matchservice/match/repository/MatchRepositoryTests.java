package ml.echelon133.matchservice.match.repository;

import ml.echelon133.common.match.MatchResult;
import ml.echelon133.common.match.MatchStatus;
import ml.echelon133.common.match.dto.MatchDto;
import ml.echelon133.matchservice.match.TestMatch;
import ml.echelon133.matchservice.match.model.Match;
import ml.echelon133.matchservice.match.model.ScoreInfo;
import ml.echelon133.matchservice.referee.model.Referee;
import ml.echelon133.matchservice.team.TestTeam;
import ml.echelon133.matchservice.venue.model.Venue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// Disable kubernetes during tests
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@DataJpaTest
public class MatchRepositoryTests {

    private final MatchRepository matchRepository;

    @Autowired
    public MatchRepositoryTests(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }



    @Test
    @DisplayName("findMatchById native query finds empty when the match does not exist")
    public void findMatchById_MatchDoesNotExist_IsEmpty() {
        var id = UUID.randomUUID();

        // when
        var matchDto = matchRepository.findMatchById(id);

        // then
        assertTrue(matchDto.isEmpty());
    }

    @Test
    @DisplayName("findMatchById native query does not fetch matches marked as deleted")
    public void findMatchById_MatchMarkedAsDeleted_IsEmpty() {
        var matchToDelete = TestMatch.builder().build();
        matchToDelete.setDeleted(true);
        var saved = matchRepository.save(matchToDelete);

        // when
        var matchDto = matchRepository.findMatchById(saved.getId());

        // then
        assertTrue(matchDto.isEmpty());
    }

    @Test
    @DisplayName("findMatchById native query finds match when the match exists")
    public void findMatchById_MatchExists_IsPresent() {
        var match = TestMatch
                .builder()
                .halfTimeScoreInfo(new ScoreInfo((byte)1, (byte)2))
                .scoreInfo(new ScoreInfo((byte)3, (byte)3))
                .result(MatchResult.DRAW)
                .status(MatchStatus.FINISHED)
                .build();
        var saved = matchRepository.save(match);

        // when
        var matchDto = matchRepository.findMatchById(saved.getId());

        // then
        assertTrue(matchDto.isPresent());
        assertEntityAndDtoEqual(match, matchDto.get());
    }

    @Test
    @DisplayName("findMatchById native query finds match when the match exists and does not leak deleted home team")
    public void findMatchById_MatchExistsAndHomeTeamDeleted_IsPresentAndDoesNotLeakDeletedEntity() {
        var teamBuilder = TestTeam.builder().deleted(true);
        var match = TestMatch.builder().homeTeam(teamBuilder).build();
        var saved = matchRepository.save(match);

        // when
        var matchDto = matchRepository.findMatchById(saved.getId());

        // then
        assertTrue(matchDto.isPresent());
        var matchDtoValue = matchDto.get();
        assertEntityAndDtoEqual(match, matchDtoValue);
        // make sure that the home team is definitely null
        assertNull(matchDtoValue.getHomeTeam());
    }

    @Test
    @DisplayName("findMatchById native query finds match when the match exists and does not leak deleted away team")
    public void findMatchById_MatchExistsAndAwayTeamDeleted_IsPresentAndDoesNotLeakDeletedEntity() {
        var teamBuilder = TestTeam.builder().deleted(true);
        var match = TestMatch.builder().awayTeam(teamBuilder).build();
        var saved = matchRepository.save(match);

        // when
        var matchDto = matchRepository.findMatchById(saved.getId());

        // then
        assertTrue(matchDto.isPresent());
        var matchDtoValue = matchDto.get();
        assertEntityAndDtoEqual(match, matchDtoValue);
        // make sure that the away team is definitely null
        assertNull(matchDtoValue.getAwayTeam());
    }

    @Test
    @DisplayName("findMatchById native query finds match when the match exists and does not leak deleted venue")
    public void findMatchById_MatchExistsAndVenueDeleted_IsPresentAndDoesNotLeakDeletedEntity() {
        var venue = new Venue("Test", null);
        venue.setDeleted(true);
        var match = TestMatch.builder().venue(venue).build();
        var saved = matchRepository.save(match);

        // when
        var matchDto = matchRepository.findMatchById(saved.getId());

        // then
        assertTrue(matchDto.isPresent());
        var matchDtoValue = matchDto.get();
        assertEntityAndDtoEqual(match, matchDtoValue);
        // make sure that the venue is definitely null
        assertNull(matchDtoValue.getVenue());
    }

    @Test
    @DisplayName("findMatchById native query finds match when the match exists and referee is set to null")
    public void findMatchById_MatchExistsAndRefereeNull_IsPresentAndContainsNullReferee() {
        var match = TestMatch.builder().referee(null).build();
        var saved = matchRepository.save(match);

        // when
        var matchDto = matchRepository.findMatchById(saved.getId());

        // then
        assertTrue(matchDto.isPresent());
        var matchDtoValue = matchDto.get();
        assertEntityAndDtoEqual(match, matchDtoValue);
        // make sure that the referee is definitely null
        assertNull(matchDtoValue.getReferee());
    }

    @Test
    @DisplayName("findMatchById native query finds match when the match exists and does not leak deleted referee")
    public void findMatchById_MatchExistsAndRefereeDeleted_IsPresentAndDoesNotLeakDeletedEntity() {
        var referee = new Referee("Test");
        referee.setDeleted(true);
        var match = TestMatch.builder().referee(referee).build();
        var saved = matchRepository.save(match);

        // when
        var matchDto = matchRepository.findMatchById(saved.getId());

        // then
        assertTrue(matchDto.isPresent());
        var matchDtoValue = matchDto.get();
        assertEntityAndDtoEqual(match, matchDtoValue);
        // make sure that the referee is definitely null
        assertNull(matchDtoValue.getReferee());
    }

    @Test
    @DisplayName("findMatchStatusById native query finds empty when the match does not exist")
    public void findMatchStatusById_MatchDoesNotExist_IsEmpty() {
        var id = UUID.randomUUID();

        // when
        var matchStatusDto = matchRepository.findMatchStatusById(id);

        // then
        assertTrue(matchStatusDto.isEmpty());
    }

    @Test
    @DisplayName("findMatchStatusById native query does not fetch matches marked as deleted")
    public void findMatchStatusById_MatchMarkedAsDeleted_IsEmpty() {
        var matchToDelete = TestMatch.builder().build();
        matchToDelete.setDeleted(true);
        var saved = matchRepository.save(matchToDelete);

        // when
        var matchStatusDto = matchRepository.findMatchStatusById(saved.getId());

        // then
        assertTrue(matchStatusDto.isEmpty());
    }

    @Test
    @DisplayName("findMatchStatusById native query finds match's status when the match exists")
    public void findMatchStatusById_MatchExists_IsPresent() {
        var match = TestMatch.builder().status(MatchStatus.HALF_TIME).build();
        var saved = matchRepository.save(match);

        // when
        var matchStatusDto = matchRepository.findMatchStatusById(saved.getId());

        // then
        assertTrue(matchStatusDto.isPresent());
        assertEquals(match.getStatus(), MatchStatus.valueOf(matchStatusDto.get().getStatus()));
    }

    @Test
    @DisplayName("markMatchAsDeleted native query only affects the match with specified id")
    public void markMatchAsDeleted_SpecifiedMatchId_OnlyMarksSpecifiedMatch() {
        var saved = matchRepository.save(TestMatch.builder().build());
        matchRepository.save(TestMatch.builder().build());
        matchRepository.save(TestMatch.builder().build());

        // when
        var countDeleted = matchRepository.markMatchAsDeleted(saved.getId());
        var match = matchRepository.findMatchById(saved.getId());

        // then
        assertEquals(1, countDeleted);
        assertTrue(match.isEmpty());
    }

    @Test
    @DisplayName("markMatchAsDeleted native query only affects not deleted matches")
    public void markMatchAsDeleted_MatchAlreadyMarkedAsDeleted_IsNotTouchedByQuery() {
        var matchToDelete= matchRepository.save(TestMatch.builder().deleted(true).build());

        // when
        Integer countDeleted = matchRepository.markMatchAsDeleted(matchToDelete.getId());

        // then
        assertEquals(0, countDeleted);
    }

    private static void assertEntityAndDtoEqual(Match entity, MatchDto dto) {
        // simple fields equal
        assertTrue(
                entity.getId().equals(dto.getId()) &&
                entity.getStatus().toString().equals(dto.getStatus()) &&
                entity.getCompetitionId().equals(dto.getCompetitionId()) &&
                entity.getStartTimeUTC().equals(dto.getStartTimeUTC())
        );

        // home teams equal
        var homeTeamEntity = entity.getHomeTeam();
        var shortHomeTeamDto = dto.getHomeTeam();
        if (homeTeamEntity.isDeleted()) {
            assertNull(shortHomeTeamDto);
        } else {
            assertTrue(
                    homeTeamEntity.getId().equals(shortHomeTeamDto.getId()) &&
                    homeTeamEntity.getName().equals(shortHomeTeamDto.getName()) &&
                    homeTeamEntity.getCrestUrl().equals(shortHomeTeamDto.getCrestUrl())
            );
        }

        // away teams equal
        var awayTeamEntity = entity.getAwayTeam();
        var shortAwayTeamDto = dto.getAwayTeam();
        if (awayTeamEntity.isDeleted()) {
            assertNull(shortAwayTeamDto);
        } else {
            assertTrue(
                    awayTeamEntity.getId().equals(shortAwayTeamDto.getId()) &&
                    awayTeamEntity.getName().equals(shortAwayTeamDto.getName()) &&
                    awayTeamEntity.getCrestUrl().equals(shortAwayTeamDto.getCrestUrl())
            );
        }

        // venues equal
        var venueEntity = entity.getVenue();
        var venueDto = dto.getVenue();
        if (venueEntity.isDeleted()) {
            assertNull(venueDto);
        } else {
            assertTrue(
                    venueEntity.getId().equals(venueDto.getId()) &&
                    venueEntity.getName().equals(venueDto.getName()) &&
                    venueEntity.getCapacity().equals(venueDto.getCapacity())
            );
        }

        // referees equal
        var refereeEntity = entity.getReferee();
        var refereeDto = dto.getReferee();
        if (refereeEntity == null) {
            assertNull(refereeDto);
        } else if (refereeEntity.isDeleted()) {
            assertNull(refereeDto);
        } else {
            assertTrue(
                    refereeEntity.getId().equals(refereeDto.getId()) &&
                    refereeEntity.getName().equals(refereeDto.getName())
            );
        }

        // half time scores equal
        var halfTimeScoreEntity = entity.getHalfTimeScoreInfo();
        var halfTimeScoreDto = dto.getHalfTimeScoreInfo();
        assertTrue(
                halfTimeScoreEntity.getHomeGoals().equals(halfTimeScoreDto.getHomeGoals()) &&
                halfTimeScoreDto.getAwayGoals().equals(halfTimeScoreDto.getAwayGoals())
        );

        // scores equal
        var scoreEntity = entity.getScoreInfo();
        var scoreDto = dto.getScoreInfo();
        assertTrue(
                scoreEntity.getHomeGoals().equals(scoreDto.getHomeGoals()) &&
                scoreEntity.getAwayGoals().equals(scoreDto.getAwayGoals())
        );

        // penalties equal
        var penaltiesEntity = entity.getPenaltiesInfo();
        var penaltiesDto = dto.getPenaltiesInfo();
        assertTrue(
                penaltiesEntity.getHomePenalties().equals(penaltiesDto.getHomePenalties()) &&
                penaltiesEntity.getAwayPenalties().equals(penaltiesDto.getAwayPenalties())
        );

        // results equal
        var resultDto = (dto.getResult() == null) ? null : MatchResult.valueOf(dto.getResult());
        assertEquals(entity.getResult(), resultDto);
    }
}
