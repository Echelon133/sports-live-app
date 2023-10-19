package ml.echelon133.matchservice.match.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.match.MatchStatus;
import ml.echelon133.common.match.dto.CompactMatchDto;
import ml.echelon133.matchservice.match.TestMatch;
import ml.echelon133.matchservice.match.TestMatchDto;
import ml.echelon133.matchservice.match.TestUpsertMatchDto;
import ml.echelon133.matchservice.match.model.Match;
import ml.echelon133.matchservice.match.repository.MatchRepository;
import ml.echelon133.matchservice.referee.model.Referee;
import ml.echelon133.matchservice.referee.service.RefereeService;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.team.repository.TeamRepository;
import ml.echelon133.matchservice.venue.model.Venue;
import ml.echelon133.matchservice.venue.service.VenueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.exceptions.misusing.InvalidUseOfMatchersException;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MatchServiceTests {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private VenueService venueService;

    @Mock
    private RefereeService refereeService;

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
    public void createMatch_VenueEmpty_Throws() throws ResourceNotFoundException {
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
        given(venueService.findEntityById(venueId)).willThrow(
                new ResourceNotFoundException(Venue.class, venueId)
        );

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
    public void createMatch_VenuePresentButMarkedAsDeleted_Throws() throws ResourceNotFoundException {
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
        given(venueService.findEntityById(venueId)).willThrow(
            new ResourceNotFoundException(Venue.class, venueId)
        );

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
        given(venueService.findEntityById(venueId)).willReturn(venueEntity);
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
        given(venueService.findEntityById(venueId)).willReturn(venueEntity);
        given(refereeService.findEntityById(refereeId)).willThrow(
                new ResourceNotFoundException(Referee.class, refereeId)
        );

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
        given(venueService.findEntityById(venueId)).willReturn(venueEntity);
        given(refereeService.findEntityById(refereeId)).willThrow(
                new ResourceNotFoundException(Referee.class, refereeId)
        );

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
        given(venueService.findEntityById(venueId)).willReturn(venueEntity);
        given(refereeService.findEntityById(refereeId)).willReturn(refereeEntity);
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
    public void updateMatch_VenueEmpty_Throws() throws ResourceNotFoundException {
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
        given(venueService.findEntityById(venueId)).willThrow(
                new ResourceNotFoundException(Venue.class, venueId)
        );

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
    public void updateMatch_VenuePresentButMarkedAsDeleted_Throws() throws ResourceNotFoundException {
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
        given(venueService.findEntityById(venueId)).willThrow(
                new ResourceNotFoundException(Venue.class, venueId)
        );

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
        given(venueService.findEntityById(newVenueId)).willReturn(newVenueEntity);
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
    public void updateMatch_RefereeEmpty_Throws() throws ResourceNotFoundException {
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
        given(venueService.findEntityById(venueId)).willReturn(venueEntity);
        given(refereeService.findEntityById(refereeId)).willThrow(
                new ResourceNotFoundException(Referee.class, refereeId)
        );

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
    public void updateMatch_RefereePresentButMarkedAsDeleted_Throws() throws ResourceNotFoundException {
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
        given(venueService.findEntityById(venueId)).willReturn(venueEntity);
        given(refereeService.findEntityById(refereeId)).willThrow(
                new ResourceNotFoundException(Referee.class, refereeId)
        );

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
        given(venueService.findEntityById(newVenueId)).willReturn(newVenueEntity);
        given(refereeService.findEntityById(newRefereeId)).willReturn(newRefereeEntity);
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

    @Test
    @DisplayName("findMatchesByDate correctly calculates day's time span for UTC +00:00")
    public void findMatchesByDate_UseCoordinatedUniversalTime_CalculatesCorrectTimeSpan() {
        var date = LocalDate.of(2023, 1, 1);
        var zoneOffset = ZoneOffset.of("+00:00");
        var pageable = Pageable.ofSize(5).withPage(1);
        // UTC +00:00 is just UTC, therefore conversion is straightforward
        var expectedStartUTC = LocalDateTime.of(2023, 1, 1, 0, 0);
        var expectedEndUTC = LocalDateTime.of(2023, 1, 1, 23, 59);

        // when
        matchService.findMatchesByDate(date, zoneOffset, pageable);

        // then
        verify(matchRepository).findAllBetween(eq(expectedStartUTC), eq(expectedEndUTC), eq(pageable));
    }

    @Test
    @DisplayName("findMatchesByDate correctly calculates day's time span for UTC +01:00")
    public void findMatchesByDate_UseCentralEuropeanTime_CalculatesCorrectTimeSpan() {
        var date = LocalDate.of(2023, 1, 1);
        var zoneOffset = ZoneOffset.of("+01:00");
        var pageable = Pageable.ofSize(5).withPage(1);
        // CET is UTC +01:00, therefore when the day starts in CET, it's 11PM the previous day in UTC
        var expectedStartUTC = LocalDateTime.of(2022, 12, 31, 23, 0);
        var expectedEndUTC = LocalDateTime.of(2023, 1, 1, 22, 59);

        // when
        matchService.findMatchesByDate(date, zoneOffset, pageable);

        // then
        verify(matchRepository).findAllBetween(eq(expectedStartUTC), eq(expectedEndUTC), eq(pageable));
    }

    @Test
    @DisplayName("findMatchesByDate correctly calculates day's time span for UTC -05:00")
    public void findMatchesByDate_UseEasternStandardTime_CalculatesCorrectTimeSpan() {
        var date = LocalDate.of(2023, 1, 1);
        var zoneOffset = ZoneOffset.of("-05:00");
        var pageable = Pageable.ofSize(5).withPage(1);
        // EST is UTC -05:00, therefore when the day starts in EST, it's 5AM the same day in UTC
        var expectedStartUTC = LocalDateTime.of(2023, 1, 1, 5, 0);
        var expectedEndUTC = LocalDateTime.of(2023, 1, 2, 4, 59);

        // when
        matchService.findMatchesByDate(date, zoneOffset, pageable);

        // then
        verify(matchRepository).findAllBetween(eq(expectedStartUTC), eq(expectedEndUTC), eq(pageable));
    }

    @Test
    @DisplayName("findMatchesByDate correctly calculates day's time span for UTC +05:30")
    public void findMatchesByDate_UseIndiaStandardTime_CalculatesCorrectTimeSpan() {
        var date = LocalDate.of(2023, 1, 1);
        var zoneOffset = ZoneOffset.of("+05:30");
        var pageable = Pageable.ofSize(5).withPage(1);
        // IST is UTC +5:30, therefore when the day starts in IST, it's 6:30PM the previous day in UTC
        var expectedStartUTC = LocalDateTime.of(2022, 12, 31, 18, 30);
        var expectedEndUTC = LocalDateTime.of(2023, 1, 1, 18, 29);

        // when
        matchService.findMatchesByDate(date, zoneOffset, pageable);

        // then
        verify(matchRepository).findAllBetween(eq(expectedStartUTC), eq(expectedEndUTC), eq(pageable));
    }

    @Test
    @DisplayName("findMatchesByDate correctly groups results by competitionId")
    public void findMatchesByDate_MultipleResults_GroupsResultsByCompetitionId() {
        var competition0 = UUID.randomUUID();
        var competition1 = UUID.randomUUID();
        List<CompactMatchDto> testResults = List.of(
                CompactMatchDto.builder().competitionId(competition0).build(),
                CompactMatchDto.builder().competitionId(competition1).build(),
                CompactMatchDto.builder().competitionId(competition1).build()
        );

        // given
        given(matchRepository.findAllBetween(any(), any(), any())).willReturn(testResults);

        // when
        Map<UUID, List<CompactMatchDto>> results = matchService.findMatchesByDate(
                LocalDate.now(), ZoneOffset.UTC, Pageable.unpaged()
        );

        // then
        assertEquals(2, results.size());
        // check competition0
        assertEquals(1, results.get(competition0).size());
        assertEquals(competition0, results.get(competition0).get(0).getCompetitionId());
        // check competition1
        assertEquals(2, results.get(competition1).size());
        var first = results.get(competition1).get(0);
        var second = results.get(competition1).get(1);
        assertEquals(competition1, first.getCompetitionId());
        assertEquals(competition1, second.getCompetitionId());
    }

    @Test
    @DisplayName("findMatchesByCompetition fetches finished matches when matchFinished is true")
    public void findMatchesByCompetition_MatchFinishedTrue_FetchesFinishedMatches() {
        var competitionId = UUID.randomUUID();
        var matchFinished = true;
        var pageable = Pageable.ofSize(3).withPage(6);

        // when
        matchService.findMatchesByCompetition(competitionId, matchFinished, pageable);

        // then
        verify(matchRepository).findAllByCompetitionAndStatuses(
                eq(competitionId),
                eq(MatchStatus.RESULT_TYPE_STATUSES),
                eq(pageable)
        );
    }

    @Test
    @DisplayName("findMatchesByCompetition fetches unfinished matches when matchFinished is false")
    public void findMatchesByCompetition_MatchFinishedFalse_FetchesUnfinishedMatches() {
        var competitionId = UUID.randomUUID();
        var matchFinished = false;
        var pageable = Pageable.ofSize(3).withPage(6);

        // when
        matchService.findMatchesByCompetition(competitionId, matchFinished, pageable);

        // then
        verify(matchRepository).findAllByCompetitionAndStatuses(
                eq(competitionId),
                eq(MatchStatus.FIXTURE_TYPE_STATUSES),
                eq(pageable)
        );
    }
}
