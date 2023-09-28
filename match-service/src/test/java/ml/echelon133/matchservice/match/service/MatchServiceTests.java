package ml.echelon133.matchservice.match.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.match.dto.MatchStatusDto;
import ml.echelon133.matchservice.match.TestMatch;
import ml.echelon133.matchservice.match.TestMatchDto;
import ml.echelon133.matchservice.match.TestUpsertMatchDto;
import ml.echelon133.matchservice.match.model.Match;
import ml.echelon133.matchservice.match.repository.MatchRepository;
import ml.echelon133.matchservice.referee.model.Referee;
import ml.echelon133.matchservice.referee.repository.RefereeRepository;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.team.repository.TeamRepository;
import ml.echelon133.matchservice.venue.model.Venue;
import ml.echelon133.matchservice.venue.repository.VenueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.exceptions.misusing.InvalidUseOfMatchersException;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class MatchServiceTests {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private RefereeRepository refereeRepository;

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private MatchService matchService;

    @Test
    @DisplayName("findById throws when the repository does not store an entity with the given id")
    public void findById_EntityNotPresent_Throws() {
        var matchId = UUID.randomUUID();

        // given
        given(matchRepository.findMatchById(matchId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.findById(matchId);
        }).getMessage();

        // then
        assertEquals(String.format("match %s could not be found", matchId), message);
    }

    @Test
    @DisplayName("findById returns the dto when the match is present")
    public void findById_EntityPresent_ReturnsDto() throws ResourceNotFoundException {
        var testDto = TestMatchDto.builder().build();
        var matchId = testDto.getId();

        // given
        given(matchRepository.findMatchById(matchId)).willReturn(Optional.of(testDto));

        // when
        var dto = matchService.findById(matchId);

        // then
        assertEquals(testDto, dto);
    }

    @Test
    @DisplayName("findStatusById throws when the repository does not store an entity with the given id")
    public void findStatusById_EntityNotPresent_Throws() {
        var matchId = UUID.randomUUID();

        // given
        given(matchRepository.findMatchStatusById(matchId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.findStatusById(matchId);
        }).getMessage();

        // then
        assertEquals(String.format("match %s could not be found", matchId), message);
    }

    @Test
    @DisplayName("findStatusById returns the dto when the match is present")
    public void findStatusById_EntityPresent_ReturnsDto() throws ResourceNotFoundException {
        var testDto = MatchStatusDto.from("FINISHED");
        var matchId = UUID.randomUUID();

        // given
        given(matchRepository.findMatchStatusById(matchId)).willReturn(Optional.of(testDto));

        // when
        var dto = matchService.findStatusById(matchId);

        // then
        assertEquals(testDto, dto);
    }

    @Test
    @DisplayName("markMatchAsDeleted calls repository's method and returns correct number of entries marked as deleted")
    public void markMatchAsDeleted_ProvidedId_CorrectlyCallsMethodAndReturnsCount() {
        var idToDelete = UUID.randomUUID();

        // given
        given(matchRepository.markMatchAsDeleted(idToDelete)).willReturn(1);

        // when
        Integer countDeleted = matchService.markMatchAsDeleted(idToDelete);

        // then
        assertEquals(1, countDeleted);
    }

    @Test
    @DisplayName("createMatch throws when the home team of the match does not exist")
    public void createMatch_HomeTeamEmpty_Throws() {
        var homeTeamId = UUID.randomUUID();

        // given
        given(teamRepository.findById(homeTeamId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.createMatch(TestUpsertMatchDto.builder().homeTeamId(homeTeamId.toString()).build());
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", homeTeamId), message);
    }

    @Test
    @DisplayName("createMatch throws when the home team of the match is marked as deleted")
    public void createMatch_HomeTeamPresentButMarkedAsDeleted_Throws() {
        var matchEntity = TestMatch.builder().build();
        var homeTeamEntity = matchEntity.getHomeTeam();
        var homeTeamId = homeTeamEntity.getId();
        homeTeamEntity.setDeleted(true);

        // given
        given(teamRepository.findById(homeTeamId)).willReturn(Optional.of(homeTeamEntity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.createMatch(TestUpsertMatchDto.builder().homeTeamId(homeTeamId.toString()).build());
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", homeTeamId), message);
    }

    // this method simplifies binding concrete arguments to concrete responses from mocked
    // TeamRepository's `findById` method.
    private void bindTeamIdsToTeams(Map<UUID, Optional<Team>> idToResponseMap) {
        given(teamRepository.findById(any(UUID.class))).willAnswer(inv -> {
            UUID id = inv.getArgument(0);
            if (idToResponseMap.containsKey(id)) {
                return idToResponseMap.get(id);
            } else {
                throw new InvalidUseOfMatchersException(
                        String.format("Id %s does not match any of the expected ids", id)
                );
            }
        });
    }

    @Test
    @DisplayName("createMatch throws when the away team of the match does not exist")
    public void createMatch_AwayTeamEmpty_Throws() {
        var matchEntity = TestMatch.builder().build();
        var homeTeamEntity = matchEntity.getHomeTeam();
        var homeTeamId = homeTeamEntity.getId();
        var awayTeamId = UUID.randomUUID();

        // given
        Map<UUID, Optional<Team>> teamIdToAnswerMapping = Map.of(
                homeTeamId, Optional.of(homeTeamEntity),
                awayTeamId, Optional.empty()
        );
        bindTeamIdsToTeams(teamIdToAnswerMapping);

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.createMatch(TestUpsertMatchDto.builder()
                            .homeTeamId(homeTeamId.toString())
                            .awayTeamId(awayTeamId.toString())
                            .build()
            );
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", awayTeamId), message);
    }

    @Test
    @DisplayName("createMatch throws when the away team of the match is marked as deleted")
    public void createMatch_AwayTeamPresentButMarkedAsDeleted_Throws() {
        var matchEntity = TestMatch.builder().build();
        var homeTeamEntity = matchEntity.getHomeTeam();
        var homeTeamId = homeTeamEntity.getId();
        var awayTeamEntity = matchEntity.getAwayTeam();
        var awayTeamId = awayTeamEntity.getId();
        awayTeamEntity.setDeleted(true);

        // given
        Map<UUID, Optional<Team>> teamIdToAnswerMapping = Map.of(
                homeTeamId, Optional.of(homeTeamEntity),
                awayTeamId, Optional.of(awayTeamEntity)
        );
        bindTeamIdsToTeams(teamIdToAnswerMapping);

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.createMatch(TestUpsertMatchDto.builder()
                    .homeTeamId(homeTeamId.toString())
                    .awayTeamId(awayTeamId.toString())
                    .build());
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", awayTeamId), message);
    }

    @Test
    @DisplayName("createMatch throws when the venue of the match does not exist")
    public void createMatch_VenueEmpty_Throws() {
        var matchEntity = TestMatch.builder().build();
        var homeTeamEntity = matchEntity.getHomeTeam();
        var homeTeamId = homeTeamEntity.getId();
        var awayTeamEntity = matchEntity.getAwayTeam();
        var awayTeamId = awayTeamEntity.getId();
        var venueId = UUID.randomUUID();

        // given
        Map<UUID, Optional<Team>> teamIdToAnswerMapping = Map.of(
                homeTeamId, Optional.of(homeTeamEntity),
                awayTeamId, Optional.of(awayTeamEntity)
        );
        bindTeamIdsToTeams(teamIdToAnswerMapping);
        given(venueRepository.findById(venueId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.createMatch(TestUpsertMatchDto.builder()
                    .homeTeamId(homeTeamId.toString())
                    .awayTeamId(awayTeamId.toString())
                    .venueId(venueId.toString())
                    .build()
            );
        }).getMessage();

        // then
        assertEquals(String.format("venue %s could not be found", venueId), message);
    }

    @Test
    @DisplayName("createMatch throws when the venue of the match is marked as deleted")
    public void createMatch_VenuePresentButMarkedAsDeleted_Throws() {
        var matchEntity = TestMatch.builder().build();
        var homeTeamEntity = matchEntity.getHomeTeam();
        var homeTeamId = homeTeamEntity.getId();
        var awayTeamEntity = matchEntity.getAwayTeam();
        var awayTeamId = awayTeamEntity.getId();
        var venueEntity = matchEntity.getVenue();
        var venueId = venueEntity.getId();
        venueEntity.setDeleted(true);

        // given
        Map<UUID, Optional<Team>> teamIdToAnswerMapping = Map.of(
                homeTeamId, Optional.of(homeTeamEntity),
                awayTeamId, Optional.of(awayTeamEntity)
        );
        bindTeamIdsToTeams(teamIdToAnswerMapping);
        given(venueRepository.findById(venueId)).willReturn(Optional.of(venueEntity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.createMatch(TestUpsertMatchDto.builder()
                    .homeTeamId(homeTeamId.toString())
                    .awayTeamId(awayTeamId.toString())
                    .venueId(venueId.toString())
                    .build());
        }).getMessage();

        // then
        assertEquals(String.format("venue %s could not be found", venueId), message);
    }

    @Test
    @DisplayName("createMatch returns the expected dto even when the optional referee is null")
    public void createMatch_RefereeNull_ReturnsDto() throws ResourceNotFoundException {
        var homeTeamEntity = new Team("Test team A", "", null, null);
        var homeTeamId = homeTeamEntity.getId();
        var awayTeamEntity = new Team("Test team B", "", null, null);
        var awayTeamId = awayTeamEntity.getId();
        var venueEntity = new Venue("Test venue", null);
        var venueId = venueEntity.getId();
        var competitionId = UUID.randomUUID();
        var startTimeUTC = "2023/01/01 19:00";

        var expectedMatch = new Match();
        expectedMatch.setHomeTeam(homeTeamEntity);
        expectedMatch.setAwayTeam(awayTeamEntity);
        expectedMatch.setVenue(venueEntity);
        expectedMatch.setReferee(null);
        expectedMatch.setStartTimeUTC(LocalDateTime.of(2023, 1, 1, 19, 0));
        expectedMatch.setCompetitionId(competitionId);

        // given
        Map<UUID, Optional<Team>> teamIdToAnswerMapping = Map.of(
                homeTeamId, Optional.of(homeTeamEntity),
                awayTeamId, Optional.of(awayTeamEntity)
        );
        bindTeamIdsToTeams(teamIdToAnswerMapping);
        given(venueRepository.findById(venueId)).willReturn(Optional.of(venueEntity));
        // make sure that createMatch has actually set all the fields of the match
        given(matchRepository.save(argThat(m ->
                    m.getHomeTeam().equals(homeTeamEntity) &&
                    m.getAwayTeam().equals(awayTeamEntity) &&
                    m.getVenue().equals(venueEntity) &&
                    m.getStartTimeUTC().equals(expectedMatch.getStartTimeUTC()) &&
                    m.getCompetitionId().equals(expectedMatch.getCompetitionId()) &&
                    m.getReferee() == null
            )
        )).willReturn(expectedMatch);

        // when
        var receivedDto = matchService.createMatch(TestUpsertMatchDto.builder()
                .homeTeamId(homeTeamId.toString())
                .awayTeamId(awayTeamId.toString())
                .venueId(venueId.toString())
                .refereeId(null)
                .competitionId(competitionId.toString())
                .startTimeUTC(startTimeUTC)
                .build()
        );

        // then
        assertEquals(expectedMatch.getId(), receivedDto.getId());
        assertNull(receivedDto.getReferee());
    }

    @Test
    @DisplayName("createMatch throws when the referee does not exist")
    public void createMatch_RefereeEmpty_Throws() throws ResourceNotFoundException {
        var homeTeamEntity = new Team("Test team A", "", null, null);
        var homeTeamId = homeTeamEntity.getId();
        var awayTeamEntity = new Team("Test team B", "", null, null);
        var awayTeamId = awayTeamEntity.getId();
        var venueEntity = new Venue("Test venue", null);
        var venueId = venueEntity.getId();
        var refereeId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();
        var startTimeUTC = "2023/01/01 19:00";

        // given
        Map<UUID, Optional<Team>> teamIdToAnswerMapping = Map.of(
                homeTeamId, Optional.of(homeTeamEntity),
                awayTeamId, Optional.of(awayTeamEntity)
        );
        bindTeamIdsToTeams(teamIdToAnswerMapping);
        given(venueRepository.findById(venueId)).willReturn(Optional.of(venueEntity));
        given(refereeRepository.findById(refereeId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.createMatch(TestUpsertMatchDto.builder()
                            .homeTeamId(homeTeamId.toString())
                            .awayTeamId(awayTeamId.toString())
                            .venueId(venueId.toString())
                            .refereeId(refereeId.toString())
                            .competitionId(competitionId.toString())
                            .startTimeUTC(startTimeUTC)
                            .build());
        }).getMessage();

        // then
        assertEquals(String.format("referee %s could not be found", refereeId), message);
    }

    @Test
    @DisplayName("createMatch throws when the referee of the match is marked as deleted")
    public void createMatch_RefereePresentButMarkedAsDeleted_Throws() throws ResourceNotFoundException {
        var homeTeamEntity = new Team("Test team A", "", null, null);
        var homeTeamId = homeTeamEntity.getId();
        var awayTeamEntity = new Team("Test team B", "", null, null);
        var awayTeamId = awayTeamEntity.getId();
        var venueEntity = new Venue("Test venue", null);
        var venueId = venueEntity.getId();
        var refereeEntity = new Referee("Test referee");
        refereeEntity.setDeleted(true);
        var refereeId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();
        var startTimeUTC = "2023/01/01 19:00";

        // given
        Map<UUID, Optional<Team>> teamIdToAnswerMapping = Map.of(
                homeTeamId, Optional.of(homeTeamEntity),
                awayTeamId, Optional.of(awayTeamEntity)
        );
        bindTeamIdsToTeams(teamIdToAnswerMapping);
        given(venueRepository.findById(venueId)).willReturn(Optional.of(venueEntity));
        given(refereeRepository.findById(refereeId)).willReturn(Optional.of(refereeEntity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.createMatch(TestUpsertMatchDto.builder()
                    .homeTeamId(homeTeamId.toString())
                    .awayTeamId(awayTeamId.toString())
                    .venueId(venueId.toString())
                    .refereeId(refereeId.toString())
                    .competitionId(competitionId.toString())
                    .startTimeUTC(startTimeUTC)
                    .build());
        }).getMessage();

        // then
        assertEquals(String.format("referee %s could not be found", refereeId), message);
    }

    @Test
    @DisplayName("createMatch returns the expected dto when the optional referee is not null")
    public void createMatch_RefereeNotNull_ReturnsDto() throws ResourceNotFoundException {
        var homeTeamEntity = new Team("Test team A", "", null, null);
        var homeTeamId = homeTeamEntity.getId();
        var awayTeamEntity = new Team("Test team B", "", null, null);
        var awayTeamId = awayTeamEntity.getId();
        var venueEntity = new Venue("Test venue", null);
        var venueId = venueEntity.getId();
        var refereeEntity = new Referee("Test referee");
        var refereeId = refereeEntity.getId();
        var competitionId = UUID.randomUUID();
        var startTimeUTC = "2023/01/01 19:00";

        var expectedMatch = new Match();
        expectedMatch.setHomeTeam(homeTeamEntity);
        expectedMatch.setAwayTeam(awayTeamEntity);
        expectedMatch.setVenue(venueEntity);
        expectedMatch.setReferee(refereeEntity);
        expectedMatch.setStartTimeUTC(LocalDateTime.of(2023, 1, 1, 19, 0));
        expectedMatch.setCompetitionId(competitionId);

        // given
        Map<UUID, Optional<Team>> teamIdToAnswerMapping = Map.of(
                homeTeamId, Optional.of(homeTeamEntity),
                awayTeamId, Optional.of(awayTeamEntity)
        );
        bindTeamIdsToTeams(teamIdToAnswerMapping);
        given(venueRepository.findById(venueId)).willReturn(Optional.of(venueEntity));
        given(refereeRepository.findById(refereeId)).willReturn(Optional.of(refereeEntity));
        // make sure that createMatch has actually set all the fields of the match
        given(matchRepository.save(argThat(m ->
                    m.getHomeTeam().equals(homeTeamEntity) &&
                    m.getAwayTeam().equals(awayTeamEntity) &&
                    m.getVenue().equals(venueEntity) &&
                    m.getStartTimeUTC().equals(expectedMatch.getStartTimeUTC()) &&
                    m.getCompetitionId().equals(expectedMatch.getCompetitionId()) &&
                    m.getReferee().equals(refereeEntity)
                )
        )).willReturn(expectedMatch);

        // when
        var receivedDto = matchService.createMatch(TestUpsertMatchDto.builder()
                .homeTeamId(homeTeamId.toString())
                .awayTeamId(awayTeamId.toString())
                .venueId(venueId.toString())
                .refereeId(refereeId.toString())
                .competitionId(competitionId.toString())
                .startTimeUTC(startTimeUTC)
                .build()
        );

        // then
        assertEquals(expectedMatch.getId(), receivedDto.getId());
        assertEquals(refereeEntity.getId(), receivedDto.getReferee().getId());
    }

    @Test
    @DisplayName("updateMatch throws when the match to update does not exist")
    public void updateMatch_MatchToUpdateEmpty_Throws() {
        var matchId = UUID.randomUUID();

        // given
        given(matchRepository.findById(matchId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.updateMatch(matchId, TestUpsertMatchDto.builder().build());
        }).getMessage();

        // then
        assertEquals(String.format("match %s could not be found", matchId), message);
    }

    @Test
    @DisplayName("updateMatch throws when the match to update is marked as deleted")
    public void updateMatch_MatchToUpdatePresentButMarkedAsDeleted_Throws() {
        var matchEntity = TestMatch.builder().deleted(true).build();
        var matchId = matchEntity.getId();

        // given
        given(matchRepository.findById(matchId)).willReturn(Optional.of(matchEntity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.updateMatch(matchId, TestUpsertMatchDto.builder().build());
        }).getMessage();

        // then
        assertEquals(String.format("match %s could not be found", matchId), message);
    }

    @Test
    @DisplayName("updateMatch throws when the home team of the match does not exist")
    public void updateMatch_HomeTeamEmpty_Throws() {
        var matchEntity = TestMatch.builder().build();
        var matchId = matchEntity.getId();
        var homeTeamId = UUID.randomUUID();

        // given
        given(matchRepository.findById(matchId)).willReturn(Optional.of(matchEntity));
        given(teamRepository.findById(homeTeamId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.updateMatch(
                    matchId,
                    TestUpsertMatchDto.builder().homeTeamId(homeTeamId.toString()).build()
            );
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", homeTeamId), message);
    }

    @Test
    @DisplayName("updateMatch throws when the home team of the match is marked as deleted")
    public void updateMatch_HomeTeamPresentButMarkedAsDeleted_Throws() {
        var matchEntity = TestMatch.builder().build();
        var matchId = matchEntity.getId();
        var homeTeamEntity = matchEntity.getHomeTeam();
        var homeTeamId = homeTeamEntity.getId();
        homeTeamEntity.setDeleted(true);

        // given
        given(teamRepository.findById(homeTeamId)).willReturn(Optional.of(homeTeamEntity));
        given(matchRepository.findById(matchId)).willReturn(Optional.of(matchEntity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.updateMatch(
                    matchId,
                    TestUpsertMatchDto.builder().homeTeamId(homeTeamId.toString()).build()
            );
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", homeTeamId), message);
    }

    @Test
    @DisplayName("updateMatch throws when the away team of the match does not exist")
    public void updateMatch_AwayTeamEmpty_Throws() {
        var matchEntity = TestMatch.builder().build();
        var matchId = matchEntity.getId();
        var homeTeamEntity = matchEntity.getHomeTeam();
        var homeTeamId = homeTeamEntity.getId();
        var awayTeamId = UUID.randomUUID();

        // given
        Map<UUID, Optional<Team>> teamIdToAnswerMapping = Map.of(
                homeTeamId, Optional.of(homeTeamEntity),
                awayTeamId, Optional.empty()
        );
        bindTeamIdsToTeams(teamIdToAnswerMapping);
        given(matchRepository.findById(matchId)).willReturn(Optional.of(matchEntity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.updateMatch(
                    matchId,
                    TestUpsertMatchDto.builder()
                        .homeTeamId(homeTeamId.toString())
                        .awayTeamId(awayTeamId.toString())
                        .build()
            );
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", awayTeamId), message);
    }

    @Test
    @DisplayName("updateMatch throws when the away team of the match is marked as deleted")
    public void updateMatch_AwayTeamPresentButMarkedAsDeleted_Throws() {
        var matchEntity = TestMatch.builder().build();
        var matchId = matchEntity.getId();
        var homeTeamEntity = matchEntity.getHomeTeam();
        var homeTeamId = homeTeamEntity.getId();
        var awayTeamEntity = matchEntity.getAwayTeam();
        var awayTeamId = awayTeamEntity.getId();
        awayTeamEntity.setDeleted(true);

        // given
        Map<UUID, Optional<Team>> teamIdToAnswerMapping = Map.of(
                homeTeamId, Optional.of(homeTeamEntity),
                awayTeamId, Optional.of(awayTeamEntity)
        );
        bindTeamIdsToTeams(teamIdToAnswerMapping);
        given(matchRepository.findById(matchId)).willReturn(Optional.of(matchEntity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.updateMatch(
                    matchId,
                    TestUpsertMatchDto.builder()
                        .homeTeamId(homeTeamId.toString())
                        .awayTeamId(awayTeamId.toString())
                        .build());
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", awayTeamId), message);
    }

    @Test
    @DisplayName("updateMatch throws when the venue of the match does not exist")
    public void updateMatch_VenueEmpty_Throws() {
        var matchEntity = TestMatch.builder().build();
        var matchId = matchEntity.getId();
        var homeTeamEntity = matchEntity.getHomeTeam();
        var homeTeamId = homeTeamEntity.getId();
        var awayTeamEntity = matchEntity.getAwayTeam();
        var awayTeamId = awayTeamEntity.getId();
        var venueId = UUID.randomUUID();

        // given
        Map<UUID, Optional<Team>> teamIdToAnswerMapping = Map.of(
                homeTeamId, Optional.of(homeTeamEntity),
                awayTeamId, Optional.of(awayTeamEntity)
        );
        bindTeamIdsToTeams(teamIdToAnswerMapping);
        given(matchRepository.findById(matchId)).willReturn(Optional.of(matchEntity));
        given(venueRepository.findById(venueId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.updateMatch(
                    matchId,
                    TestUpsertMatchDto.builder()
                        .homeTeamId(homeTeamId.toString())
                        .awayTeamId(awayTeamId.toString())
                        .venueId(venueId.toString())
                        .build()
            );
        }).getMessage();

        // then
        assertEquals(String.format("venue %s could not be found", venueId), message);
    }

    @Test
    @DisplayName("updateMatch throws when the venue of the match is marked as deleted")
    public void updateMatch_VenuePresentButMarkedAsDeleted_Throws() {
        var matchEntity = TestMatch.builder().build();
        var matchId = matchEntity.getId();
        var homeTeamEntity = matchEntity.getHomeTeam();
        var homeTeamId = homeTeamEntity.getId();
        var awayTeamEntity = matchEntity.getAwayTeam();
        var awayTeamId = awayTeamEntity.getId();
        var venueEntity = matchEntity.getVenue();
        var venueId = venueEntity.getId();
        venueEntity.setDeleted(true);

        // given
        Map<UUID, Optional<Team>> teamIdToAnswerMapping = Map.of(
                homeTeamId, Optional.of(homeTeamEntity),
                awayTeamId, Optional.of(awayTeamEntity)
        );
        bindTeamIdsToTeams(teamIdToAnswerMapping);
        given(matchRepository.findById(matchId)).willReturn(Optional.of(matchEntity));
        given(venueRepository.findById(venueId)).willReturn(Optional.of(venueEntity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.updateMatch(
                    matchId,
                    TestUpsertMatchDto.builder()
                        .homeTeamId(homeTeamId.toString())
                        .awayTeamId(awayTeamId.toString())
                        .venueId(venueId.toString())
                        .build());
        }).getMessage();

        // then
        assertEquals(String.format("venue %s could not be found", venueId), message);
    }

    @Test
    @DisplayName("updateMatch returns the expected dto even when the optional referee is null")
    public void updateMatch_RefereeNull_ReturnsDto() throws ResourceNotFoundException {
        var matchEntity = TestMatch.builder().referee(null).build();
        var matchId = matchEntity.getId();

        var newHomeTeamEntity = new Team("Test team A", "", null, null);
        var newHomeTeamId = newHomeTeamEntity.getId();
        var newAwayTeamEntity = new Team("Test team B", "", null, null);
        var newAwayTeamId = newAwayTeamEntity.getId();
        var newVenueEntity = new Venue("Test venue", null);
        var newVenueId = newVenueEntity.getId();
        var newCompetitionId = UUID.randomUUID();
        var newStartTimeUTC = "2023/01/01 19:00";

        var expectedMatch = new Match();
        expectedMatch.setId(matchId);
        expectedMatch.setHomeTeam(newHomeTeamEntity);
        expectedMatch.setAwayTeam(newAwayTeamEntity);
        expectedMatch.setVenue(newVenueEntity);
        expectedMatch.setReferee(null);
        expectedMatch.setStartTimeUTC(LocalDateTime.of(2023, 1, 1, 19, 0));
        expectedMatch.setCompetitionId(newCompetitionId);

        // given
        Map<UUID, Optional<Team>> teamIdToAnswerMapping = Map.of(
                newHomeTeamId, Optional.of(newHomeTeamEntity),
                newAwayTeamId, Optional.of(newAwayTeamEntity)
        );
        bindTeamIdsToTeams(teamIdToAnswerMapping);
        given(matchRepository.findById(matchId)).willReturn(Optional.of(matchEntity));
        given(venueRepository.findById(newVenueId)).willReturn(Optional.of(newVenueEntity));
        // make sure that updateMatch has actually set all the fields of the match
        given(matchRepository.save(argThat(m ->
                        m.getId().equals(matchEntity.getId()) &&
                        m.getHomeTeam().equals(newHomeTeamEntity) &&
                        m.getAwayTeam().equals(newAwayTeamEntity) &&
                        m.getVenue().equals(newVenueEntity) &&
                        m.getStartTimeUTC().equals(expectedMatch.getStartTimeUTC()) &&
                        m.getCompetitionId().equals(expectedMatch.getCompetitionId()) &&
                        m.getReferee() == null
                )
        )).willReturn(expectedMatch);

        // when
        var receivedDto = matchService.updateMatch(
                matchId,
                TestUpsertMatchDto.builder()
                    .homeTeamId(newHomeTeamId.toString())
                    .awayTeamId(newAwayTeamId.toString())
                    .venueId(newVenueId.toString())
                    .refereeId(null)
                    .competitionId(newCompetitionId.toString())
                    .startTimeUTC(newStartTimeUTC)
                    .build()
        );

        // then
        assertEquals(expectedMatch.getId(), receivedDto.getId());
        assertNull(receivedDto.getReferee());
    }

    @Test
    @DisplayName("updateMatch throws when the referee does not exist")
    public void updateMatch_RefereeEmpty_Throws() {
        var matchEntity = TestMatch.builder().referee(null).build();
        var matchId = matchEntity.getId();

        var homeTeamEntity = new Team("Test team A", "", null, null);
        var homeTeamId = homeTeamEntity.getId();
        var awayTeamEntity = new Team("Test team B", "", null, null);
        var awayTeamId = awayTeamEntity.getId();
        var venueEntity = new Venue("Test venue", null);
        var venueId = venueEntity.getId();
        var refereeId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();
        var startTimeUTC = "2023/01/01 19:00";

        // given
        Map<UUID, Optional<Team>> teamIdToAnswerMapping = Map.of(
                homeTeamId, Optional.of(homeTeamEntity),
                awayTeamId, Optional.of(awayTeamEntity)
        );
        bindTeamIdsToTeams(teamIdToAnswerMapping);
        given(matchRepository.findById(matchId)).willReturn(Optional.of(matchEntity));
        given(venueRepository.findById(venueId)).willReturn(Optional.of(venueEntity));
        given(refereeRepository.findById(refereeId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.updateMatch(
                    matchId,
                    TestUpsertMatchDto.builder()
                        .homeTeamId(homeTeamId.toString())
                        .awayTeamId(awayTeamId.toString())
                        .venueId(venueId.toString())
                        .refereeId(refereeId.toString())
                        .competitionId(competitionId.toString())
                        .startTimeUTC(startTimeUTC)
                        .build()
            );
        }).getMessage();

        // then
        assertEquals(String.format("referee %s could not be found", refereeId), message);
    }

    @Test
    @DisplayName("updateMatch throws when the referee of the match is marked as deleted")
    public void updateMatch_RefereePresentButMarkedAsDeleted_Throws() {
        var matchEntity = TestMatch.builder().referee(null).build();
        var matchId = matchEntity.getId();

        var homeTeamEntity = new Team("Test team A", "", null, null);
        var homeTeamId = homeTeamEntity.getId();
        var awayTeamEntity = new Team("Test team B", "", null, null);
        var awayTeamId = awayTeamEntity.getId();
        var venueEntity = new Venue("Test venue", null);
        var venueId = venueEntity.getId();
        var refereeEntity = new Referee("Test referee");
        refereeEntity.setDeleted(true);
        var refereeId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();
        var startTimeUTC = "2023/01/01 19:00";

        // given
        Map<UUID, Optional<Team>> teamIdToAnswerMapping = Map.of(
                homeTeamId, Optional.of(homeTeamEntity),
                awayTeamId, Optional.of(awayTeamEntity)
        );
        bindTeamIdsToTeams(teamIdToAnswerMapping);
        given(matchRepository.findById(matchId)).willReturn(Optional.of(matchEntity));
        given(venueRepository.findById(venueId)).willReturn(Optional.of(venueEntity));
        given(refereeRepository.findById(refereeId)).willReturn(Optional.of(refereeEntity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.updateMatch(
                    matchId,
                    TestUpsertMatchDto.builder()
                        .homeTeamId(homeTeamId.toString())
                        .awayTeamId(awayTeamId.toString())
                        .venueId(venueId.toString())
                        .refereeId(refereeId.toString())
                        .competitionId(competitionId.toString())
                        .startTimeUTC(startTimeUTC)
                        .build()
            );
        }).getMessage();

        // then
        assertEquals(String.format("referee %s could not be found", refereeId), message);
    }

    @Test
    @DisplayName("updateMatch returns the expected dto when the optional referee is not null")
    public void updateMatch_RefereeNotNull_ReturnsDto() throws ResourceNotFoundException {
        var matchEntity = TestMatch.builder().referee(null).build();
        var matchId = matchEntity.getId();

        var newHomeTeamEntity = new Team("Test team A", "", null, null);
        var newHomeTeamId = newHomeTeamEntity.getId();
        var newAwayTeamEntity = new Team("Test team B", "", null, null);
        var newAwayTeamId = newAwayTeamEntity.getId();
        var newVenueEntity = new Venue("Test venue", null);
        var newVenueId = newVenueEntity.getId();
        var newRefereeEntity = new Referee("Test referee");
        var newRefereeId = newRefereeEntity.getId();
        var newCompetitionId = UUID.randomUUID();
        var newStartTimeUTC = "2023/01/01 19:00";

        var expectedMatch = new Match();
        expectedMatch.setId(matchId);
        expectedMatch.setHomeTeam(newHomeTeamEntity);
        expectedMatch.setAwayTeam(newAwayTeamEntity);
        expectedMatch.setVenue(newVenueEntity);
        expectedMatch.setReferee(newRefereeEntity);
        expectedMatch.setStartTimeUTC(LocalDateTime.of(2023, 1, 1, 19, 0));
        expectedMatch.setCompetitionId(newCompetitionId);

        // given
        Map<UUID, Optional<Team>> teamIdToAnswerMapping = Map.of(
                newHomeTeamId, Optional.of(newHomeTeamEntity),
                newAwayTeamId, Optional.of(newAwayTeamEntity)
        );
        bindTeamIdsToTeams(teamIdToAnswerMapping);
        given(matchRepository.findById(matchId)).willReturn(Optional.of(matchEntity));
        given(venueRepository.findById(newVenueId)).willReturn(Optional.of(newVenueEntity));
        given(refereeRepository.findById(newRefereeId)).willReturn(Optional.of(newRefereeEntity));
        // make sure that updateMatch has actually set all the fields of the match
        given(matchRepository.save(argThat(m ->
                        m.getId().equals(matchId) &&
                        m.getHomeTeam().equals(newHomeTeamEntity) &&
                        m.getAwayTeam().equals(newAwayTeamEntity) &&
                        m.getVenue().equals(newVenueEntity) &&
                        m.getStartTimeUTC().equals(expectedMatch.getStartTimeUTC()) &&
                        m.getCompetitionId().equals(expectedMatch.getCompetitionId()) &&
                        m.getReferee().equals(newRefereeEntity)
                )
        )).willReturn(expectedMatch);

        // when
        var receivedDto = matchService.updateMatch(
                matchId,
                TestUpsertMatchDto.builder()
                    .homeTeamId(newHomeTeamId.toString())
                    .awayTeamId(newAwayTeamId.toString())
                    .venueId(newVenueId.toString())
                    .refereeId(newRefereeId.toString())
                    .competitionId(newCompetitionId.toString())
                    .startTimeUTC(newStartTimeUTC)
                    .build()
        );

        // then
        assertEquals(expectedMatch.getId(), receivedDto.getId());
        assertEquals(newRefereeEntity.getId(), receivedDto.getReferee().getId());
    }
}
