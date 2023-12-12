package ml.echelon133.matchservice.match.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.match.MatchStatus;
import ml.echelon133.common.match.dto.CompactMatchDto;
import ml.echelon133.common.team.dto.TeamPlayerDto;
import ml.echelon133.matchservice.match.TestMatch;
import ml.echelon133.matchservice.match.TestMatchDto;
import ml.echelon133.matchservice.match.TestUpsertMatchDto;
import ml.echelon133.matchservice.match.exceptions.LineupPlayerInvalidException;
import ml.echelon133.matchservice.match.model.Match;
import ml.echelon133.matchservice.match.model.UpsertLineupDto;
import ml.echelon133.matchservice.match.repository.MatchRepository;
import ml.echelon133.matchservice.referee.model.Referee;
import ml.echelon133.matchservice.referee.service.RefereeService;
import ml.echelon133.matchservice.team.TestTeamPlayerDto;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.team.model.TeamPlayer;
import ml.echelon133.matchservice.team.service.TeamPlayerService;
import ml.echelon133.matchservice.team.service.TeamService;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MatchServiceTests {

    @Mock
    private TeamService teamService;

    @Mock
    private TeamPlayerService teamPlayerService;

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
    @DisplayName("findEntityById throws when the repository does not store an entity with given id")
    public void findEntityById_EntityNotPresent_Throws() {
        var testId = UUID.randomUUID();

        // given
        given(matchRepository.findById(testId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.findEntityById(testId);
        }).getMessage();

        // then
        assertEquals(String.format("match %s could not be found", testId), message);
    }

    @Test
    @DisplayName("findEntityById throws when the repository stores an entity with given id but it's deleted")
    public void findEntityById_EntityPresentButDeleted_Throws() {
        var testId = UUID.randomUUID();
        var matchEntity = new Match();
        matchEntity.setDeleted(true);

        // given
        given(matchRepository.findById(testId)).willReturn(Optional.of(matchEntity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.findEntityById(testId);
        }).getMessage();

        // then
        assertEquals(String.format("match %s could not be found", testId), message);
    }

    @Test
    @DisplayName("findEntityById returns the entity when the repository stores it")
    public void findEntityById_EntityPresent_ReturnsEntity() throws ResourceNotFoundException {
        var testId = UUID.randomUUID();
        var matchEntity = new Match();

        // given
        given(matchRepository.findById(testId)).willReturn(Optional.of(matchEntity));

        // when
        var entity = matchService.findEntityById(testId);

        // then
        assertEquals(matchEntity, entity);
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
    public void createMatch_HomeTeamEmpty_Throws() throws ResourceNotFoundException {
        var homeTeamId = UUID.randomUUID();

        // given
        given(teamService.findEntityById(homeTeamId)).willThrow(
                new ResourceNotFoundException(Team.class, homeTeamId)
        );

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.createMatch(TestUpsertMatchDto.builder().homeTeamId(homeTeamId.toString()).build());
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", homeTeamId), message);
    }

    // this method simplifies binding concrete arguments to concrete responses from mocked
    // TeamService's `findEntityById` method.
    private void bindTeamIdsToTeams(Map<UUID, Optional<Team>> idToResponseMap) throws ResourceNotFoundException {
        given(teamService.findEntityById(any(UUID.class))).willAnswer(inv -> {
            UUID id = inv.getArgument(0);
            if (idToResponseMap.containsKey(id)) {
                return idToResponseMap.get(id)
                        .filter(t -> !t.isDeleted())
                        .orElseThrow(() -> new ResourceNotFoundException(Team.class, id)
                );
            } else {
                throw new InvalidUseOfMatchersException(
                        String.format("Id %s does not match any of the expected ids", id)
                );
            }
        });
    }

    @Test
    @DisplayName("createMatch throws when the away team of the match does not exist")
    public void createMatch_AwayTeamEmpty_Throws() throws ResourceNotFoundException {
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
    public void updateMatch_HomeTeamEmpty_Throws() throws ResourceNotFoundException {
        var matchEntity = TestMatch.builder().build();
        var matchId = matchEntity.getId();
        var homeTeamId = UUID.randomUUID();

        // given
        given(matchRepository.findById(matchId)).willReturn(Optional.of(matchEntity));
        given(teamService.findEntityById(homeTeamId)).willThrow(
                new ResourceNotFoundException(Team.class, homeTeamId)
        );

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
    public void updateMatch_AwayTeamEmpty_Throws() throws ResourceNotFoundException {
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

    @Test
    @DisplayName("findMatchLineup correctly fetches match lineups")
    public void findMatchLineup_MatchLineupsExist_CorrectlyFetchesLineups() {
        var matchId = UUID.randomUUID();

        var homeStartingPlayer = TestTeamPlayerDto.builder().build();
        var homeSubstitutePlayer= TestTeamPlayerDto.builder().build();
        var awayStartingPlayer= TestTeamPlayerDto.builder().build();
        var awaySubstitutePlayer= TestTeamPlayerDto.builder().build();

        // given
        given(matchRepository.findHomeStartingPlayersByMatchId(matchId)).willReturn(List.of(homeStartingPlayer));
        given(matchRepository.findHomeSubstitutePlayersByMatchId(matchId)).willReturn(List.of(homeSubstitutePlayer));
        given(matchRepository.findAwayStartingPlayersByMatchId(matchId)).willReturn(List.of(awayStartingPlayer));
        given(matchRepository.findAwaySubstitutePlayersByMatchId(matchId)).willReturn(List.of(awaySubstitutePlayer));

        // when
        var lineup = matchService.findMatchLineup(matchId);

        // then
        var homeLineup = lineup.getHome();
        var awayLineup = lineup.getAway();
        assertEquals(homeStartingPlayer.getId(), homeLineup.getStartingPlayers().get(0).getId());
        assertEquals(homeSubstitutePlayer.getId(), homeLineup.getSubstitutePlayers().get(0).getId());
        assertEquals(awayStartingPlayer.getId(), awayLineup.getStartingPlayers().get(0).getId());
        assertEquals(awaySubstitutePlayer.getId(), awayLineup.getSubstitutePlayers().get(0).getId());
    }

    @Test
    @DisplayName("updateHomeLineup throws when the match does not exist")
    public void updateHomeLineup_MatchDoesNotExist_Throws() {
        var matchId = UUID.randomUUID();

        // given
        given(matchRepository.findById(matchId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.updateHomeLineup(matchId, new UpsertLineupDto());
        }).getMessage();

        // then
        assertEquals(String.format("match %s could not be found", matchId), message);
    }

    @Test
    @DisplayName("updateHomeLineup throws when the match exists but is marked as deleted")
    public void updateHomeLineup_MatchExistsButMarkedAsDeleted_Throws() {
        var match = TestMatch.builder().deleted(true).build();
        var matchId = match.getId();

        // given
        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.updateHomeLineup(matchId, new UpsertLineupDto());
        }).getMessage();

        // then
        assertEquals(String.format("match %s could not be found", matchId), message);
    }

    @Test
    @DisplayName("updateHomeLineup throws when the starting lineup contains players who do not play for the team")
    public void updateHomeLineup_StartingLineupContainsInvalidPlayers_Throws() {
        var match = TestMatch.builder().build();
        var matchId = match.getId();
        var homeTeamId = match.getHomeTeam().getId();

        // players who play for the home team
        List<TeamPlayerDto> validHomeTeamPlayers = List.of(
                TestTeamPlayerDto.builder().playerName("Player A").build(),
                TestTeamPlayerDto.builder().playerName("Player B").build()
        );

        // have two ids of players who play for the home team and one random id
        // representing a player from outside the team
        var startingHomeTeamPlayers = validHomeTeamPlayers
                .stream().map(p -> p.getId().toString()).collect(Collectors.toList());
        startingHomeTeamPlayers.add(UUID.randomUUID().toString());

        // given
        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));
        given(teamPlayerService.findAllPlayersOfTeam(homeTeamId)).willReturn(validHomeTeamPlayers);

        // when
        String message = assertThrows(LineupPlayerInvalidException.class, () -> {
            matchService.updateHomeLineup(matchId, new UpsertLineupDto(
                    startingHomeTeamPlayers, List.of()
            ));
        }).getMessage();

        // then
        assertEquals("at least one of provided starting players does not play for this team", message);
    }

    @Test
    @DisplayName("updateHomeLineup throws when the substitute lineup contains players who do not play for the team")
    public void updateHomeLineup_SubstituteLineupContainsInvalidPlayers_Throws() {
        var match = TestMatch.builder().build();
        var matchId = match.getId();
        var homeTeamId = match.getHomeTeam().getId();

        // players who play for the home team
        List<TeamPlayerDto> validHomeTeamPlayers = List.of(
                TestTeamPlayerDto.builder().playerName("Player A").build(),
                TestTeamPlayerDto.builder().playerName("Player B").build()
        );

        // have two ids of players who play for the home team and one random id
        // representing a player from outside the team
        var substituteHomeTeamPlayers = validHomeTeamPlayers
                .stream().map(p -> p.getId().toString()).collect(Collectors.toList());
        substituteHomeTeamPlayers.add(UUID.randomUUID().toString());

        // given
        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));
        given(teamPlayerService.findAllPlayersOfTeam(homeTeamId)).willReturn(validHomeTeamPlayers);

        // when
        String message = assertThrows(LineupPlayerInvalidException.class, () -> {
            matchService.updateHomeLineup(matchId, new UpsertLineupDto(
                    List.of(), substituteHomeTeamPlayers
            ));
        }).getMessage();

        // then
        assertEquals("at least one of provided substitute players does not play for this team", message);
    }

    @Test
    @DisplayName("updateHomeLineup saves the lineup when all of the players play for the team")
    public void updateHomeLineup_LineupContainsOnlyValidPlayers_SavesLineup()
            throws LineupPlayerInvalidException, ResourceNotFoundException {
        var match = TestMatch.builder().build();
        var matchId = match.getId();
        var homeTeamId = match.getHomeTeam().getId();

        // players who play for the home team
        List<TeamPlayerDto> validHomeTeamPlayers = List.of(
                TestTeamPlayerDto.builder().playerName("Player A").build(),
                TestTeamPlayerDto.builder().playerName("Player B").build(),
                TestTeamPlayerDto.builder().playerName("Player C").build(),
                TestTeamPlayerDto.builder().playerName("Player D").build()
        );

        var startingHomeTeamPlayers = validHomeTeamPlayers.subList(0, 3)
                .stream().map(p -> p.getId().toString()).collect(Collectors.toList());
        var substituteHomeTeamPlayers = validHomeTeamPlayers.subList(3, 4)
                .stream().map(p -> p.getId().toString()).collect(Collectors.toList());

        // given
        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));
        given(teamPlayerService.findAllPlayersOfTeam(homeTeamId)).willReturn(validHomeTeamPlayers);
        // simulate turning a list of ids into a list of entity references
        given(teamPlayerService.mapAllIdsToReferences(anyList())).willAnswer(inv -> {
            List<UUID> ids = inv.getArgument(0);
            return ids.stream().map(id -> {
                var teamPlayer = new TeamPlayer();
                teamPlayer.setId(id);
                return teamPlayer;
            }).collect(Collectors.toList());
        });

        // when
        matchService.updateHomeLineup(matchId, new UpsertLineupDto(
                startingHomeTeamPlayers, substituteHomeTeamPlayers
        ));

        // then
        verify(matchRepository).save(argThat(m ->
                m.getHomeLineup().getStartingPlayers().size() == 3 &&
                m.getHomeLineup().getSubstitutePlayers().size() == 1 &&
                m.getAwayLineup().getStartingPlayers().size() == 0 &&
                m.getAwayLineup().getSubstitutePlayers().size() == 0
        ));
    }

    @Test
    @DisplayName("updateAwayLineup throws when the match does not exist")
    public void updateAwayLineup_MatchDoesNotExist_Throws() {
        var matchId = UUID.randomUUID();

        // given
        given(matchRepository.findById(matchId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.updateAwayLineup(matchId, new UpsertLineupDto());
        }).getMessage();

        // then
        assertEquals(String.format("match %s could not be found", matchId), message);
    }

    @Test
    @DisplayName("updateAwayLineup throws when the match exists but is marked as deleted")
    public void updateAwayLineup_MatchExistsButMarkedAsDeleted_Throws() {
        var match = TestMatch.builder().deleted(true).build();
        var matchId = match.getId();

        // given
        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchService.updateAwayLineup(matchId, new UpsertLineupDto());
        }).getMessage();

        // then
        assertEquals(String.format("match %s could not be found", matchId), message);
    }

    @Test
    @DisplayName("updateAwayLineup throws when the starting lineup contains players who do not play for the team")
    public void updateAwayLineup_StartingLineupContainsInvalidPlayers_Throws() {
        var match = TestMatch.builder().build();
        var matchId = match.getId();
        var awayTeamId = match.getAwayTeam().getId();

        // players who play for the away team
        List<TeamPlayerDto> validAwayTeamPlayers = List.of(
                TestTeamPlayerDto.builder().playerName("Player A").build(),
                TestTeamPlayerDto.builder().playerName("Player B").build()
        );

        // have two ids of players who play for the away team and one random id
        // representing a player from outside the team
        var startingAwayTeamPlayers = validAwayTeamPlayers
                .stream().map(p -> p.getId().toString()).collect(Collectors.toList());
        startingAwayTeamPlayers.add(UUID.randomUUID().toString());

        // given
        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));
        given(teamPlayerService.findAllPlayersOfTeam(awayTeamId)).willReturn(validAwayTeamPlayers);

        // when
        String message = assertThrows(LineupPlayerInvalidException.class, () -> {
            matchService.updateAwayLineup(matchId, new UpsertLineupDto(
                    startingAwayTeamPlayers, List.of()
            ));
        }).getMessage();

        // then
        assertEquals("at least one of provided starting players does not play for this team", message);
    }

    @Test
    @DisplayName("updateAwayLineup throws when the substitute lineup contains players who do not play for the team")
    public void updateAwayLineup_SubstituteLineupContainsInvalidPlayers_Throws() {
        var match = TestMatch.builder().build();
        var matchId = match.getId();
        var awayTeamId = match.getAwayTeam().getId();

        // players who play for the away team
        List<TeamPlayerDto> validAwayTeamPlayers = List.of(
                TestTeamPlayerDto.builder().playerName("Player A").build(),
                TestTeamPlayerDto.builder().playerName("Player B").build()
        );

        // have two ids of players who play for the away team and one random id
        // representing a player from outside the team
        var substituteAwayTeamPlayers = validAwayTeamPlayers
                .stream().map(p -> p.getId().toString()).collect(Collectors.toList());
        substituteAwayTeamPlayers.add(UUID.randomUUID().toString());

        // given
        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));
        given(teamPlayerService.findAllPlayersOfTeam(awayTeamId)).willReturn(validAwayTeamPlayers);

        // when
        String message = assertThrows(LineupPlayerInvalidException.class, () -> {
            matchService.updateAwayLineup(matchId, new UpsertLineupDto(
                    List.of(), substituteAwayTeamPlayers
            ));
        }).getMessage();

        // then
        assertEquals("at least one of provided substitute players does not play for this team", message);
    }

    @Test
    @DisplayName("updateAwayLineup saves the lineup when all of the players play for the team")
    public void updateAwayLineup_LineupContainsOnlyValidPlayers_SavesLineup()
            throws LineupPlayerInvalidException, ResourceNotFoundException {
        var match = TestMatch.builder().build();
        var matchId = match.getId();
        var awayTeamId = match.getAwayTeam().getId();

        // players who play for the away team
        List<TeamPlayerDto> validAwayTeamPlayers = List.of(
                TestTeamPlayerDto.builder().playerName("Player A").build(),
                TestTeamPlayerDto.builder().playerName("Player B").build(),
                TestTeamPlayerDto.builder().playerName("Player C").build(),
                TestTeamPlayerDto.builder().playerName("Player D").build()
        );

        var startingAwayTeamPlayers = validAwayTeamPlayers.subList(0, 3)
                .stream().map(p -> p.getId().toString()).collect(Collectors.toList());
        var substituteAwayTeamPlayers = validAwayTeamPlayers.subList(3, 4)
                .stream().map(p -> p.getId().toString()).collect(Collectors.toList());

        // given
        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));
        given(teamPlayerService.findAllPlayersOfTeam(awayTeamId)).willReturn(validAwayTeamPlayers);
        // simulate turning a list of ids into a list of entity references
        given(teamPlayerService.mapAllIdsToReferences(anyList())).willAnswer(inv -> {
            List<UUID> ids = inv.getArgument(0);
            return ids.stream().map(id -> {
                var teamPlayer = new TeamPlayer();
                teamPlayer.setId(id);
                return teamPlayer;
            }).collect(Collectors.toList());
        });

        // when
        matchService.updateAwayLineup(matchId, new UpsertLineupDto(
                startingAwayTeamPlayers, substituteAwayTeamPlayers
        ));

        // then
        verify(matchRepository).save(argThat(m ->
                m.getHomeLineup().getStartingPlayers().size() == 0 &&
                m.getHomeLineup().getSubstitutePlayers().size() == 0 &&
                m.getAwayLineup().getStartingPlayers().size() == 3 &&
                m.getAwayLineup().getSubstitutePlayers().size() == 1
        ));
    }
}
