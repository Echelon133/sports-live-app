package ml.echelon133.matchservice.match.repository;

import ml.echelon133.common.entity.BaseEntity;
import ml.echelon133.common.match.MatchResult;
import ml.echelon133.common.match.MatchStatus;
import ml.echelon133.common.match.dto.CompactMatchDto;
import ml.echelon133.common.match.dto.MatchDto;
import ml.echelon133.common.team.dto.TeamPlayerDto;
import ml.echelon133.matchservice.match.TestMatch;
import ml.echelon133.matchservice.match.TestMatchLineup;
import ml.echelon133.matchservice.match.model.Match;
import ml.echelon133.matchservice.match.model.ScoreInfo;
import ml.echelon133.matchservice.referee.model.Referee;
import ml.echelon133.matchservice.team.TestTeam;
import ml.echelon133.matchservice.team.model.TeamPlayer;
import ml.echelon133.matchservice.venue.model.Venue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
                .halfTimeScoreInfo(new ScoreInfo(1, 2))
                .scoreInfo(new ScoreInfo(3, 3))
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

    @Test
    @DisplayName("findAllBetween native query does not fetch matches marked as deleted")
    public void findAllBetween_MatchMarkedAsDeleted_SizeIsZero() {
        // start the match 2023/01/01 8:00AM
        var startDate = LocalDateTime.of(2023, 1, 1, 8, 0);
        var matchToDelete = TestMatch.builder()
                .startTimeUTC(startDate)
                .deleted(true)
                .build();
        matchRepository.save(matchToDelete);

        // when
        var result = matchRepository.findAllBetween(
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2023, 1, 1, 23, 59),
                Pageable.unpaged()
        );

        // then
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("findAllBetween native query does not leak deleted home team of a found match")
    public void findAllBetween_HomeTeamDeleted_DoesNotLeakDeletedEntity() {
        var teamBuilder = TestTeam.builder().deleted(true);
        // start the match 2023/01/01 8:00AM
        var startDate = LocalDateTime.of(2023, 1, 1, 8, 0);
        var match = TestMatch.builder().startTimeUTC(startDate).homeTeam(teamBuilder).build();
        matchRepository.save(match);

        // when
        var result = matchRepository.findAllBetween(
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2023, 1, 1, 23, 59),
                Pageable.unpaged()
        );

        // then
        assertEquals(1, result.size());
        var compactMatchDtoValue = result.get(0);
        assertEntityAndDtoEqual(match, compactMatchDtoValue);
        // make sure that the home team is definitely null
        assertNull(compactMatchDtoValue.getHomeTeam());
    }

    @Test
    @DisplayName("findAllBetween native query does not leak deleted away team of a found match")
    public void findAllBetween_AwayTeamDeleted_DoesNotLeakDeletedEntity() {
        var teamBuilder = TestTeam.builder().deleted(true);
        // start the match 2023/01/01 8:00AM
        var startDate = LocalDateTime.of(2023, 1, 1, 8, 0);
        var match = TestMatch.builder().startTimeUTC(startDate).awayTeam(teamBuilder).build();
        matchRepository.save(match);

        // when
        var result = matchRepository.findAllBetween(
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2023, 1, 1, 23, 59),
                Pageable.unpaged()
        );

        // then
        assertEquals(1, result.size());
        var compactMatchDtoValue = result.get(0);
        assertEntityAndDtoEqual(match, compactMatchDtoValue);
        // make sure that the away team is definitely null
        assertNull(compactMatchDtoValue.getAwayTeam());
    }

    @Test
    @DisplayName("findAllBetween native query only finds matches that start between two specified dates")
    public void findAllBetween_MultipleMatches_OnlyFindsMatchesBetweenTwoDates() {
        // create four matches:
        //  0 -> starts 2023/01/01 00:00
        var startTime0 = LocalDateTime.of(2023, 1, 1, 0, 0);
        //  1 -> starts 2023/01/01 11:59PM
        var startTime1 = LocalDateTime.of(2023, 1, 1, 23, 59);
        //  2 -> starts 2023/01/02 00:00
        var startTime2 = LocalDateTime.of(2023, 1, 2, 0, 0);
        //  3 -> starts 2023/01/02 11:59PM
        var startTime3 = LocalDateTime.of(2023, 1, 2, 23, 59);

        var match0 = matchRepository.save(TestMatch.builder().startTimeUTC(startTime0).build());
        var match1 = matchRepository.save(TestMatch.builder().startTimeUTC(startTime1).build());
        var match2 = matchRepository.save(TestMatch.builder().startTimeUTC(startTime2).build());
        var match3 = matchRepository.save(TestMatch.builder().startTimeUTC(startTime3).build());

        // when
        // find matches that started on 2023/01/01 (should return match0 and match1)
        var result0 = matchRepository.findAllBetween(
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2023, 1, 1, 23, 59),
                Pageable.unpaged()
        );
        // find matches that started on 2023/01/02 (should return match2 and match3)
        var result1 = matchRepository.findAllBetween(
                LocalDateTime.of(2023, 1, 2, 0, 0),
                LocalDateTime.of(2023, 1, 2, 23, 59),
                Pageable.unpaged()
        );
        // find matches between 2023/01/01 and 2023/01/04 (should return all matches)
        var result2 = matchRepository.findAllBetween(
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2023, 1, 4, 0, 0),
                Pageable.unpaged()
        );

        // then
        // result0 should only contain match0 and match1
        assertEquals(
                2,
                result0.stream().filter(m ->
                        m.getId().equals(match0.getId()) || m.getId().equals(match1.getId())
                ).count()
        );
        // result1 should only contain match2 and match3
        assertEquals(
                2,
                result1.stream().filter(m ->
                        m.getId().equals(match2.getId()) || m.getId().equals(match3.getId())
                ).count()
        );
        // result2 should contain all four matches
        assertEquals(4, result2.size());
    }

    @Test
    @DisplayName("findAllBetween native query takes pageable into account")
    public void findAllBetween_CustomPageable_ResultsPaged() {
        var startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        // create four random test matches
        IntStream.range(0, 4)
                .mapToObj(i -> TestMatch.builder().startTimeUTC(startDate).build())
                .forEach(matchRepository::save);

        // when
        var pageable = Pageable.ofSize(1);
        // first page should return 1 match
        var result0 = matchRepository.findAllBetween(
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2023, 1, 1, 23, 59),
                pageable
        );
        // fourth page should return 1 match
        var result1 = matchRepository.findAllBetween(
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2023, 1, 1, 23, 59),
                pageable.withPage(3)
        );
        // fifth page should be empty
        var result2 = matchRepository.findAllBetween(
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2023, 1, 1, 23, 59),
                pageable.withPage(4)
        );

        // then
        assertEquals(1, result0.size());
        assertEquals(1, result1.size());
        assertEquals(0, result2.size());
    }

    @Test
    @DisplayName("findAllByCompetitionAndStatuses native query does not fetch matches marked as deleted")
    public void findAllByCompetitionAndStatuses_MatchMarkedAsDeleted_SizeIsZero() {
        var competitionId = UUID.randomUUID();
        var matchToDelete = TestMatch.builder()
                .status(MatchStatus.NOT_STARTED)
                .competitionId(competitionId)
                .deleted(true)
                .build();
        matchRepository.save(matchToDelete);

        // when
        var result = matchRepository.findAllByCompetitionAndStatuses(
                competitionId, MatchStatus.ALL_STATUSES, Pageable.unpaged()
        );

        // then
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("findAllByCompetitionAndStatuses native query only fetches matches from the specified competition")
    public void findAllByCompetitionAndStatuses_MultipleCompetitions_OnlyFetchesSpecificCompetition() {
        var competition0 = UUID.randomUUID();
        var competition1 = UUID.randomUUID();
        var match0 = TestMatch.builder().competitionId(competition0).build();
        var match1 = TestMatch.builder().competitionId(competition1).build();
        matchRepository.saveAll(List.of(match0, match1));

        // when
        var result0 = matchRepository.findAllByCompetitionAndStatuses(
                competition0, MatchStatus.ALL_STATUSES, Pageable.unpaged()
        );
        var result1 = matchRepository.findAllByCompetitionAndStatuses(
                competition1, MatchStatus.ALL_STATUSES, Pageable.unpaged()
        );

        // then
        assertEquals(
                1,
                result0.stream().filter(m -> m.getCompetitionId().equals(competition0)).count()
        );
        assertEquals(
                1,
                result1.stream().filter(m -> m.getCompetitionId().equals(competition1)).count()
        );
    }

    @Test
    @DisplayName("findAllByCompetitionAndStatuses native query does not leak deleted home team of a found match")
    public void findAllByCompetitionAndStatuses_HomeTeamDeleted_DoesNotLeakDeletedEntity() {
        var competitionId = UUID.randomUUID();
        var teamBuilder = TestTeam.builder().deleted(true);
        var match = TestMatch.builder()
                .competitionId(competitionId)
                .homeTeam(teamBuilder)
                .build();
        matchRepository.save(match);

        // when
        var result = matchRepository.findAllByCompetitionAndStatuses(
                competitionId, MatchStatus.ALL_STATUSES, Pageable.unpaged()
        );

        // then
        assertEquals(1, result.size());
        var compactMatchDtoValue = result.get(0);
        assertEntityAndDtoEqual(match, compactMatchDtoValue);
        // make sure that the home team is definitely null
        assertNull(compactMatchDtoValue.getHomeTeam());
    }

    @Test
    @DisplayName("findAllByCompetitionAndStatuses native query does not leak deleted away team of a found match")
    public void findAllByCompetitionAndStatuses_AwayTeamDeleted_DoesNotLeakDeletedEntity() {
        var competitionId = UUID.randomUUID();
        var teamBuilder = TestTeam.builder().deleted(true);
        var match = TestMatch.builder()
                .competitionId(competitionId)
                .awayTeam(teamBuilder)
                .build();
        matchRepository.save(match);

        // when
        var result = matchRepository.findAllByCompetitionAndStatuses(
                competitionId, MatchStatus.ALL_STATUSES, Pageable.unpaged()
        );

        // then
        assertEquals(1, result.size());
        var compactMatchDtoValue = result.get(0);
        assertEntityAndDtoEqual(match, compactMatchDtoValue);
        // make sure that the away team is definitely null
        assertNull(compactMatchDtoValue.getAwayTeam());
    }

    @Test
    @DisplayName("findAllByCompetitionAndStatuses native query filters matches by their status")
    public void findAllByCompetitionAndStatuses_MultipleMatchesWithDifferentStatuses_FiltersMatchesByStatus() {
        var competitionId = UUID.randomUUID();
        for (MatchStatus status : MatchStatus.values()) {
            matchRepository.save(
                    TestMatch.builder().competitionId(competitionId).status(status).build()
            );
        }

        // when
        // all matches (should contain 9 matches)
        var allMatches = matchRepository.findAllByCompetitionAndStatuses(
                competitionId, MatchStatus.ALL_STATUSES, Pageable.unpaged()
        );
        // finished matches (should contain 2 matches)
        var finishedMatches = matchRepository.findAllByCompetitionAndStatuses(
                competitionId, MatchStatus.RESULT_TYPE_STATUSES, Pageable.unpaged()
        );
        // unfinished matches (should contain 7 matches)
        var unfinishedMatches= matchRepository.findAllByCompetitionAndStatuses(
                competitionId, MatchStatus.FIXTURE_TYPE_STATUSES, Pageable.unpaged()
        );

        // then
        assertEquals(9, allMatches.size());
        assertEquals(2, finishedMatches.size());
        assertEquals(7, unfinishedMatches.size());
    }

    @Test
    @DisplayName("findAllByCompetitionAndStatuses native query takes pageable into account")
    public void findAllByCompetitionAndStatuses_CustomPageable_ResultsPaged() {
        var competitionId = UUID.randomUUID();

        // create four random test matches
        IntStream.range(0, 4)
                .mapToObj(i -> TestMatch.builder().competitionId(competitionId).build())
                .forEach(matchRepository::save);

        // when
        var pageable = Pageable.ofSize(1);
        // first page should return 1 match
        var result0 = matchRepository.findAllByCompetitionAndStatuses(
                competitionId, MatchStatus.ALL_STATUSES, pageable
        );
        // fourth page should return 1 match
        var result1 = matchRepository.findAllByCompetitionAndStatuses(
                competitionId, MatchStatus.ALL_STATUSES, pageable.withPage(3)
        );
        // fifth page should be empty
        var result2 = matchRepository.findAllByCompetitionAndStatuses(
                competitionId, MatchStatus.ALL_STATUSES, pageable.withPage(4)
        );

        // then
        assertEquals(1, result0.size());
        assertEquals(1, result1.size());
        assertEquals(0, result2.size());
    }

    @Test
    @DisplayName("findAllByCompetitionAndStatuses native query sorts by date descending and time ascending")
    public void findAllByCompetitionAndStatuses_MultipleMatches_SortDateDescendingTimeAscending() {
        var competitionId = UUID.randomUUID();
        var day0 = LocalDate.of(2022, 1, 1);
        var day1 = LocalDate.of(2023, 1, 10);
        var day2 = LocalDate.of(2024, 12, 31);

        // create two matches on day2
        Stream.of(10, 22).forEach(hour -> matchRepository.save(TestMatch.builder()
                .startTimeUTC(LocalDateTime.of(day2, LocalTime.of(hour, 0)))
                .competitionId(competitionId)
                .build()
        ));
        // create two matches on day0
        Stream.of(7, 9).forEach(hour -> matchRepository.save(TestMatch.builder()
                .startTimeUTC(LocalDateTime.of(day0, LocalTime.of(hour, 0)))
                .competitionId(competitionId)
                .build()
        ));
        // create two matches on day1
        Stream.of(15, 19).forEach(hour -> matchRepository.save(TestMatch.builder()
                .startTimeUTC(LocalDateTime.of(day1, LocalTime.of(hour, 0)))
                .competitionId(competitionId)
                .build()
        ));

        // when
        var results = matchRepository.findAllByCompetitionAndStatuses(
                competitionId, MatchStatus.ALL_STATUSES, Pageable.unpaged()
        );

        // then
        // expected order:
        //  0  day2 10:00AM
        //  1  day2 10:00PM
        //  2  day1 03:00PM
        //  3  day1 07:00PM
        //  4  day0 07:00AM
        //  5  day0 09:00AM
        assertEquals(
                LocalDateTime.of(day2, LocalTime.of(10, 0)),
                results.get(0).getStartTimeUTC()
        );
        assertEquals(
                LocalDateTime.of(day2, LocalTime.of(22, 0)),
                results.get(1).getStartTimeUTC()
        );
        assertEquals(
                LocalDateTime.of(day1, LocalTime.of(15, 0)),
                results.get(2).getStartTimeUTC()
        );
        assertEquals(
                LocalDateTime.of(day1, LocalTime.of(19, 0)),
                results.get(3).getStartTimeUTC()
        );
        assertEquals(
                LocalDateTime.of(day0, LocalTime.of(7, 0)),
                results.get(4).getStartTimeUTC()
        );
        assertEquals(
                LocalDateTime.of(day0, LocalTime.of(9, 0)),
                results.get(5).getStartTimeUTC()
        );
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
                penaltiesEntity.getHomeGoals().equals(penaltiesDto.getHomeGoals()) &&
                penaltiesEntity.getAwayGoals().equals(penaltiesDto.getAwayGoals())
        );

        // results equal
        var resultDto = (dto.getResult() == null) ? null : MatchResult.valueOf(dto.getResult());
        assertEquals(entity.getResult(), resultDto);
    }

    private static void assertEntityAndDtoEqual(Match entity, CompactMatchDto dto) {
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
                penaltiesEntity.getHomeGoals().equals(penaltiesDto.getHomeGoals()) &&
                penaltiesEntity.getAwayGoals().equals(penaltiesDto.getAwayGoals())
        );

        // results equal
        var resultDto = (dto.getResult() == null) ? null : MatchResult.valueOf(dto.getResult());
        assertEquals(entity.getResult(), resultDto);
    }

    @Test
    @DisplayName("findHomeStartingPlayersByMatchId native query returns empty list if match does not exist")
    public void findHomeStartingPlayersByMatchId_MatchDoesNotExist_IsEmpty() {
        var matchId = UUID.randomUUID();

        // when
        var homeStartingPlayers = matchRepository.findHomeStartingPlayersByMatchId(matchId);

        // then
        assertEquals(0, homeStartingPlayers.size());
    }

    @Test
    @DisplayName("findHomeStartingPlayersByMatchId native query returns empty list if match is marked as deleted")
    public void findHomeStartingPlayersByMatchId_MatchExistsButMarkedAsDeleted_IsEmpty() {
        var match = TestMatchLineup.createTestMatchWithLineup();
        match.setDeleted(true);
        match = matchRepository.save(match);
        var matchId = match.getId();

        // when
        var homeStartingPlayers = matchRepository.findHomeStartingPlayersByMatchId(matchId);

        // then
        assertEquals(0, homeStartingPlayers.size());
    }

    @Test
    @DisplayName("findHomeStartingPlayersByMatchId native query returns players from particular lineup")
    public void findHomeStartingPlayersByMatchId_MultipleExistingMatches_OnlyFetchesParticularLineup() {
        var match1 = matchRepository.save(TestMatchLineup.createTestMatchWithLineup());
        var match2 = matchRepository.save(TestMatchLineup.createTestMatchWithLineup());
        var match1Id = match1.getId();
        var match2Id = match2.getId();

        // when
        var homeStartingPlayers1 = matchRepository.findHomeStartingPlayersByMatchId(match1Id);
        var homeStartingPlayers2 = matchRepository.findHomeStartingPlayersByMatchId(match2Id);

        // then
        assertLineupContainsPlayers(homeStartingPlayers1, match1.getHomeLineup().getStartingPlayers());
        assertLineupContainsPlayers(homeStartingPlayers2, match2.getHomeLineup().getStartingPlayers());
    }

    @Test
    @DisplayName("findHomeSubstitutePlayersByMatchId native query returns empty list if match does not exist")
    public void findHomeSubstitutePlayersByMatchId_MatchDoesNotExist_IsEmpty() {
        var matchId = UUID.randomUUID();

        // when
        var homeSubstitutePlayers = matchRepository.findHomeSubstitutePlayersByMatchId(matchId);

        // then
        assertEquals(0, homeSubstitutePlayers.size());
    }

    @Test
    @DisplayName("findHomeSubstitutePlayersByMatchId native query returns empty list if match is marked as deleted")
    public void findHomeSubstitutePlayersByMatchId_MatchExistsButMarkedAsDeleted_IsEmpty() {
        var match = TestMatchLineup.createTestMatchWithLineup();
        match.setDeleted(true);
        match = matchRepository.save(match);
        var matchId = match.getId();

        // when
        var homeSubstitutePlayers = matchRepository.findHomeSubstitutePlayersByMatchId(matchId);

        // then
        assertEquals(0, homeSubstitutePlayers.size());
    }

    @Test
    @DisplayName("findHomeSubstitutePlayersByMatchId native query returns players from particular lineup")
    public void findHomeSubstitutePlayersByMatchId_MultipleExistingMatches_OnlyFetchesParticularLineup() {
        var match1 = matchRepository.save(TestMatchLineup.createTestMatchWithLineup());
        var match2 = matchRepository.save(TestMatchLineup.createTestMatchWithLineup());
        var match1Id = match1.getId();
        var match2Id = match2.getId();

        // when
        var homeSubstitutePlayers1 = matchRepository.findHomeSubstitutePlayersByMatchId(match1Id);
        var homeSubstitutePlayers2 = matchRepository.findHomeSubstitutePlayersByMatchId(match2Id);

        // then
        assertLineupContainsPlayers(homeSubstitutePlayers1, match1.getHomeLineup().getSubstitutePlayers());
        assertLineupContainsPlayers(homeSubstitutePlayers2, match2.getHomeLineup().getSubstitutePlayers());
    }

    @Test
    @DisplayName("findAwayStartingPlayersByMatchId native query returns empty list if match does not exist")
    public void findAwayStartingPlayersByMatchId_MatchDoesNotExist_IsEmpty() {
        var matchId = UUID.randomUUID();

        // when
        var awayStartingPlayers = matchRepository.findAwayStartingPlayersByMatchId(matchId);

        // then
        assertEquals(0, awayStartingPlayers.size());
    }

    @Test
    @DisplayName("findAwayStartingPlayersByMatchId native query returns empty list if match is marked as deleted")
    public void findAwayStartingPlayersByMatchId_MatchExistsButMarkedAsDeleted_IsEmpty() {
        var match = TestMatchLineup.createTestMatchWithLineup();
        match.setDeleted(true);
        match = matchRepository.save(match);
        var matchId = match.getId();

        // when
        var awayStartingPlayers = matchRepository.findAwayStartingPlayersByMatchId(matchId);

        // then
        assertEquals(0, awayStartingPlayers.size());
    }

    @Test
    @DisplayName("findAwayStartingPlayersByMatchId native query returns players from particular lineup")
    public void findAwayStartingPlayersByMatchId_MultipleExistingMatches_OnlyFetchesParticularLineup() {
        var match1 = matchRepository.save(TestMatchLineup.createTestMatchWithLineup());
        var match2 = matchRepository.save(TestMatchLineup.createTestMatchWithLineup());
        var match1Id = match1.getId();
        var match2Id = match2.getId();

        // when
        var awayStartingPlayers1 = matchRepository.findAwayStartingPlayersByMatchId(match1Id);
        var awayStartingPlayers2 = matchRepository.findAwayStartingPlayersByMatchId(match2Id);

        // then
        assertLineupContainsPlayers(awayStartingPlayers1, match1.getAwayLineup().getStartingPlayers());
        assertLineupContainsPlayers(awayStartingPlayers2, match2.getAwayLineup().getStartingPlayers());
    }

    @Test
    @DisplayName("findAwaySubstitutePlayersByMatchId native query returns empty list if match does not exist")
    public void findAwaySubstitutePlayersByMatchId_MatchDoesNotExist_IsEmpty() {
        var matchId = UUID.randomUUID();

        // when
        var awaySubstitutePlayers = matchRepository.findAwaySubstitutePlayersByMatchId(matchId);

        // then
        assertEquals(0, awaySubstitutePlayers.size());
    }

    @Test
    @DisplayName("findAwaySubstitutePlayersByMatchId native query returns empty list if match is marked as deleted")
    public void findAwaySubstitutePlayersByMatchId_MatchExistsButMarkedAsDeleted_IsEmpty() {
        var match = TestMatchLineup.createTestMatchWithLineup();
        match.setDeleted(true);
        match = matchRepository.save(match);
        var matchId = match.getId();

        // when
        var awaySubstitutePlayers = matchRepository.findAwaySubstitutePlayersByMatchId(matchId);

        // then
        assertEquals(0, awaySubstitutePlayers.size());
    }

    @Test
    @DisplayName("findAwaySubstitutePlayersByMatchId native query returns players from particular lineup")
    public void findAwaySubstitutePlayersByMatchId_MultipleExistingMatches_OnlyFetchesParticularLineup() {
        var match1 = matchRepository.save(TestMatchLineup.createTestMatchWithLineup());
        var match2 = matchRepository.save(TestMatchLineup.createTestMatchWithLineup());
        var match1Id = match1.getId();
        var match2Id = match2.getId();

        // when
        var awaySubstitutePlayers1 = matchRepository.findAwaySubstitutePlayersByMatchId(match1Id);
        var awaySubstitutePlayers2 = matchRepository.findAwaySubstitutePlayersByMatchId(match2Id);

        // then
        assertLineupContainsPlayers(awaySubstitutePlayers1, match1.getAwayLineup().getSubstitutePlayers());
        assertLineupContainsPlayers(awaySubstitutePlayers2, match2.getAwayLineup().getSubstitutePlayers());
    }

    private static void assertLineupContainsPlayers(List<TeamPlayerDto> foundLineup, List<TeamPlayer> expectedLineup) {
        var foundIds = foundLineup.stream().map(TeamPlayerDto::getId).collect(Collectors.toList());
        var expectedIds = expectedLineup.stream().map(BaseEntity::getId).collect(Collectors.toList());
        assertTrue(foundIds.containsAll(expectedIds));
    }
}
