package ml.echelon133.matchservice.event.service;

import ml.echelon133.common.event.dto.MatchEventDetails;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.match.MatchStatus;
import ml.echelon133.common.match.dto.LineupDto;
import ml.echelon133.matchservice.event.exceptions.MatchEventInvalidException;
import ml.echelon133.matchservice.event.model.MatchEvent;
import ml.echelon133.matchservice.event.model.dto.InsertMatchEvent;
import ml.echelon133.matchservice.event.repository.MatchEventRepository;
import ml.echelon133.matchservice.match.TestMatch;
import ml.echelon133.matchservice.match.model.Match;
import ml.echelon133.matchservice.match.service.MatchService;
import ml.echelon133.matchservice.player.model.Player;
import ml.echelon133.matchservice.player.model.Position;
import ml.echelon133.matchservice.team.TestTeam;
import ml.echelon133.matchservice.team.TestTeamPlayerDto;
import ml.echelon133.matchservice.team.model.TeamPlayer;
import ml.echelon133.matchservice.team.service.TeamPlayerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.exceptions.misusing.InvalidUseOfMatchersException;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MatchEventServiceTests {

    @Mock
    private MatchEventRepository matchEventRepository;

    @Mock
    private MatchService matchService;

    @Mock
    private TeamPlayerService teamPlayerService;

    @InjectMocks
    private MatchEventService matchEventService;

    private MatchEvent createTestStatusMatchEvent(UUID matchId, String minute, MatchStatus targetStatus) {
        var match = TestMatch.builder().id(matchId).build();
        return new MatchEvent(match, new MatchEventDetails.StatusDto(minute, match.getCompetitionId(), targetStatus));
    }

    // this method simplifies binding concrete arguments to concrete responses from mocked
    // TeamPlayerService's `findEntityById` method.
    private void bindTeamPlayerIdsToTeamPlayers(Map<UUID, Optional<TeamPlayer>> idToResponseMap) throws ResourceNotFoundException {
        given(teamPlayerService.findEntityById(any(UUID.class))).willAnswer(inv -> {
            UUID id = inv.getArgument(0);
            if (idToResponseMap.containsKey(id)) {
                return idToResponseMap.get(id)
                        .filter(t -> !t.isDeleted())
                        .orElseThrow(() -> new ResourceNotFoundException(TeamPlayer.class, id)
                        );
            } else {
                throw new InvalidUseOfMatchersException(
                        String.format("Id %s does not match any of the expected ids", id)
                );
            }
        });
    }

    @Test
    @DisplayName("findAllByMatchId returns an empty list when there are no events")
    public void findAllByMatchId_NoEvents_ReturnsEmptyList() {
        var matchId = UUID.randomUUID();

        // given
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(List.of());

        // when
        var events = matchEventService.findAllByMatchId(matchId);

        // then
        assertEquals(0, events.size());
    }

    @Test
    @DisplayName("findAllByMatchId returns expected status event dtos")
    public void findAllByMatchId_HasStatusEvent_ReturnsDto() {
        var matchId = UUID.randomUUID();
        var targetStatus = MatchStatus.FIRST_HALF;
        var statusEvent = createTestStatusMatchEvent(matchId, "1", targetStatus);

        // given
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId))
                .willReturn(List.of(statusEvent));

        // when
        var events = matchEventService.findAllByMatchId(matchId);

        // then
        assertEquals(
                1,
                events.stream().filter(e -> {
                    var innerEvent = (MatchEventDetails.StatusDto)e.getEvent();
                    return e.getId().equals(statusEvent.getId()) &&
                            innerEvent.getMinute().equals(statusEvent.getEvent().getMinute()) &&
                            innerEvent.getTargetStatus().equals(targetStatus);
                }).count()
        );
    }

    @Test
    @DisplayName("processEvent throws when the match does not exist")
    public void processEvent_MatchNotFound_Throws() throws ResourceNotFoundException {
        var matchId = UUID.randomUUID();
        var eventDto = new InsertMatchEvent.StatusDto("1", "asdf");

        // given
        given(matchService.findEntityById(matchId)).willThrow(new ResourceNotFoundException(Match.class, matchId));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            matchEventService.processEvent(matchId, eventDto);
        }).getMessage();

        // then
        assertEquals(String.format("match %s could not be found", matchId), message);
    }

    @Test
    @DisplayName("processEvent of NOT_STARTED match only processes valid STATUS changes")
    public void processEvent_MatchStatusNotStarted_RejectsInvalidStatusChanges() throws ResourceNotFoundException {
        var testedMatchStatus = MatchStatus.NOT_STARTED;
        var expectedValidStatusChanges = List.of(
                MatchStatus.FIRST_HALF, MatchStatus.ABANDONED, MatchStatus.POSTPONED
        );
        assertMatchStatusCanOnlyChangeTo(testedMatchStatus, expectedValidStatusChanges);
    }

    @Test
    @DisplayName("processEvent of FIRST_HALF match only processes valid STATUS changes")
    public void processEvent_MatchStatusFirstHalf_RejectsInvalidStatusChanges() throws ResourceNotFoundException {
        var testedMatchStatus = MatchStatus.FIRST_HALF;
        var expectedValidStatusChanges = List.of(
                MatchStatus.HALF_TIME, MatchStatus.ABANDONED
        );
        assertMatchStatusCanOnlyChangeTo(testedMatchStatus, expectedValidStatusChanges);
    }

    @Test
    @DisplayName("processEvent of HALF_TIME match only processes valid STATUS changes")
    public void processEvent_MatchStatusHalfTime_RejectsInvalidStatusChanges() throws ResourceNotFoundException {
        var testedMatchStatus = MatchStatus.HALF_TIME;
        var expectedValidStatusChanges = List.of(
                MatchStatus.SECOND_HALF, MatchStatus.ABANDONED
        );
        assertMatchStatusCanOnlyChangeTo(testedMatchStatus, expectedValidStatusChanges);
    }

    @Test
    @DisplayName("processEvent of SECOND_HALF match only processes valid STATUS changes")
    public void processEvent_MatchStatusSecondHalf_RejectsInvalidStatusChanges() throws ResourceNotFoundException {
        var testedMatchStatus = MatchStatus.SECOND_HALF;
        var expectedValidStatusChanges = List.of(
                MatchStatus.FINISHED, MatchStatus.EXTRA_TIME, MatchStatus.ABANDONED
        );
        assertMatchStatusCanOnlyChangeTo(testedMatchStatus, expectedValidStatusChanges);
    }

    @Test
    @DisplayName("processEvent of EXTRA_TIME match only processes valid STATUS changes")
    public void processEvent_MatchStatusExtraTime_RejectsInvalidStatusChanges() throws ResourceNotFoundException {
        var testedMatchStatus = MatchStatus.EXTRA_TIME;
        var expectedValidStatusChanges = List.of(
                MatchStatus.FINISHED, MatchStatus.PENALTIES, MatchStatus.ABANDONED
        );
        assertMatchStatusCanOnlyChangeTo(testedMatchStatus, expectedValidStatusChanges);
    }

    @Test
    @DisplayName("processEvent of PENALTIES match only processes valid STATUS changes")
    public void processEvent_MatchStatusPenalties_RejectsInvalidStatusChanges() throws ResourceNotFoundException {
        var testedMatchStatus = MatchStatus.PENALTIES;
        var expectedValidStatusChanges = List.of(
                MatchStatus.FINISHED, MatchStatus.ABANDONED
        );
        assertMatchStatusCanOnlyChangeTo(testedMatchStatus, expectedValidStatusChanges);
    }

    @Test
    @DisplayName("processEvent of POSTPONED match only processes valid STATUS changes")
    public void processEvent_MatchStatusPostponed_RejectsInvalidStatusChanges() throws ResourceNotFoundException {
        var testedMatchStatus = MatchStatus.POSTPONED;
        List<MatchStatus> expectedValidStatusChanges = List.of();
        assertMatchStatusCanOnlyChangeTo(testedMatchStatus, expectedValidStatusChanges);
    }

    @Test
    @DisplayName("processEvent of ABANDONED match only processes valid STATUS changes")
    public void processEvent_MatchStatusAbandoned_RejectsInvalidStatusChanges() throws ResourceNotFoundException {
        var testedMatchStatus = MatchStatus.ABANDONED;
        List<MatchStatus> expectedValidStatusChanges = List.of();
        assertMatchStatusCanOnlyChangeTo(testedMatchStatus, expectedValidStatusChanges);
    }

    // helper which makes sure that a match with some initial MatchStatus cannot be moved into an invalid
    // status (i.e. a match that is already FINISHED cannot be marked as NOT_STARTED, etc.)
    private void assertMatchStatusCanOnlyChangeTo(
            MatchStatus testedMatchStatus,
            List<MatchStatus> expectedValidStatusChanges
    ) throws ResourceNotFoundException {

        var match = new Match();
        match.setStatus(testedMatchStatus);
        var matchId = match.getId();

        List<MatchStatus> nonRejectedStatuses = new ArrayList<>();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);

        // when
        // send every possible match status update and collect these which didn't fail
        for (MatchStatus attemptedTargetStatus : MatchStatus.values()) {
            // reset the original match status before every attempt
            match.setStatus(testedMatchStatus);
            var testedEvent = new InsertMatchEvent.StatusDto("1", attemptedTargetStatus.name());

            try {
                matchEventService.processEvent(matchId, testedEvent);
                nonRejectedStatuses.add(attemptedTargetStatus);
                // make sure that the actual match object got its status changed when
                // the event processing was successful
                assertEquals(attemptedTargetStatus, match.getStatus());
            } catch (MatchEventInvalidException ignore) {
                // make sure that the match object's status remains unchanged
                assertEquals(testedMatchStatus, match.getStatus());
            }
        }

        // then
        assertTrue(
                expectedValidStatusChanges.size() == nonRejectedStatuses.size() &&
                nonRejectedStatuses.containsAll(expectedValidStatusChanges)
        );
        verify(matchEventRepository, times(expectedValidStatusChanges.size())).save(
                argThat(e -> e.getMatch().getId().equals(matchId))
        );
    }

    @Test
    @DisplayName("processEvent saves the commentary")
    public void processEvent_CommentaryPresent_SavesEvent() throws MatchEventInvalidException, ResourceNotFoundException {
        var match = new Match();
        var matchId = match.getId();
        var message = "This is a test message";
        var testedEvent = new InsertMatchEvent.CommentaryDto("45", message);

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);

        // when
        matchEventService.processEvent(matchId, testedEvent);

        // then
        verify(matchEventRepository).save(argThat(matchEvent -> {
            MatchEventDetails.CommentaryDto eventDetails = (MatchEventDetails.CommentaryDto)matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) && eventDetails.getMessage().equals(message);
        }));
    }

    @Test
    @DisplayName("processEvent rejects CARD events if the ball in the match is not in play")
    public void processEvent_BallNotInPlay_RejectsInvalidCard() throws ResourceNotFoundException {
        var ballNotInPlayStatuses = List.of(
                MatchStatus.NOT_STARTED, MatchStatus.HALF_TIME, MatchStatus.POSTPONED, MatchStatus.ABANDONED
        );
        var match = new Match();
        var matchId = match.getId();
        var testEvent = new InsertMatchEvent.CardDto("1", UUID.randomUUID().toString(), false);

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);

        for (MatchStatus status: ballNotInPlayStatuses) {
            match.setStatus(status);

            // when
            String message = assertThrows(MatchEventInvalidException.class, () -> {
                matchEventService.processEvent(matchId, testEvent);
            }).getMessage();

            // then
            assertEquals("event cannot be processed when the ball is not in play", message);
        }
    }

    @Test
    @DisplayName("processEvent rejects CARD events if the player does not play for either team")
    public void processEvent_PlayerDoesNotPlayForTeams_RejectsInvalidCard() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // make sure that the player plays for a team that is not in the match
        var testTeamPlayer = new TeamPlayer(
                TestTeam.builder().name("Team C").build(),
                new Player(),
                Position.GOALKEEPER,
                1
        );
        var testTeamPlayerId = testTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.CardDto("1", testTeamPlayerId.toString(), false);

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(testTeamPlayerId)).willReturn(testTeamPlayer);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s does not play for either team", testTeamPlayerId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects CARD events if the player plays for the team in the match but is not in the lineup")
    public void processEvent_PlayerNotInLineup_RejectsInvalidCard() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // make sure that the player plays for a team from the match
        var testTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var testTeamPlayerId = testTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.CardDto("1", testTeamPlayerId.toString(), false);
        // create an empty lineup which ensures that the player won't be in it
        var teamLineup = new LineupDto();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(testTeamPlayerId)).willReturn(testTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s is not placed in the lineup of this match", testTeamPlayerId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent accepts a CARD event if a player with no cards gets a yellow card")
    public void processEvent_PlayerWithNoCardsGetsYellow_SavesEvent() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // make sure that the player plays for a team from the match
        var testTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var testTeamPlayerId = testTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.CardDto("1", testTeamPlayerId.toString(), false);
        // put the player in the starting home lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(testTeamPlayerId).build()),
                List.of(),
                List.of(),
                List.of()
        );
        // past events do not contain any card events for the player
        List<MatchEvent> pastEvents = List.of();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(testTeamPlayerId)).willReturn(testTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        verify(matchEventRepository).save(argThat(matchEvent -> {
            MatchEventDetails.CardDto cDto = (MatchEventDetails.CardDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    cDto.getCardType().equals(MatchEventDetails.CardDto.CardType.YELLOW);
        }));
    }

    @Test
    @DisplayName("processEvent accepts a CARD event if a player with no cards gets a red card")
    public void processEvent_PlayerWithNoCardsGetsRed_SavesEvent() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // make sure that the player plays for a team from the match
        var testTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var testTeamPlayerId = testTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.CardDto("1", testTeamPlayerId.toString(), true);
        // put the player in the starting home lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(testTeamPlayerId).build()),
                List.of(),
                List.of(),
                List.of()
        );
        // past events do not contain any card events for the player
        List<MatchEvent> pastEvents = List.of();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(testTeamPlayerId)).willReturn(testTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        verify(matchEventRepository).save(argThat(matchEvent -> {
            MatchEventDetails.CardDto cDto = (MatchEventDetails.CardDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    cDto.getCardType().equals(MatchEventDetails.CardDto.CardType.DIRECT_RED);
        }));
    }

    @Test
    @DisplayName("processEvent accepts a CARD event if a player with a yellow card gets a second yellow card")
    public void processEvent_PlayerWithYellowGetsSecondYellow_SavesEvent() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // make sure that the player plays for a team from the match
        var testTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var testTeamPlayerId = testTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.CardDto("1", testTeamPlayerId.toString(), false);
        // put the player in the starting home lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(testTeamPlayerId).build()),
                List.of(),
                List.of(),
                List.of()
        );
        // create a past event with a yellow card for the player
        List<MatchEvent> pastEvents = List.of(new MatchEvent(
                match,
                new MatchEventDetails.CardDto(
                        "45",
                        UUID.randomUUID(),
                        testTeamPlayer.getTeam().getId(),
                        MatchEventDetails.CardDto.CardType.YELLOW,
                        new MatchEventDetails.SerializedPlayerInfo(testTeamPlayerId, null, null)
                )
        ));

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(testTeamPlayerId)).willReturn(testTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        verify(matchEventRepository).save(argThat(matchEvent -> {
            MatchEventDetails.CardDto cDto = (MatchEventDetails.CardDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    cDto.getCardType().equals(MatchEventDetails.CardDto.CardType.SECOND_YELLOW);
        }));
    }

    @Test
    @DisplayName("processEvent accepts a CARD event if a player with a yellow card gets a direct red card")
    public void processEvent_PlayerWithYellowGetsDirectRed_SavesEvent() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // make sure that the player plays for a team from the match
        var testTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var testTeamPlayerId = testTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.CardDto("1", testTeamPlayerId.toString(), true);
        // put the player in the starting home lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(testTeamPlayerId).build()),
                List.of(),
                List.of(),
                List.of()
        );
        // create a past event with a yellow card for the player
        List<MatchEvent> pastEvents = List.of(new MatchEvent(
                match,
                new MatchEventDetails.CardDto(
                        "45",
                        UUID.randomUUID(),
                        null,
                        MatchEventDetails.CardDto.CardType.YELLOW,
                        new MatchEventDetails.SerializedPlayerInfo(testTeamPlayerId, null, null)
                )
        ));

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(testTeamPlayerId)).willReturn(testTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        verify(matchEventRepository).save(argThat(matchEvent -> {
            MatchEventDetails.CardDto cDto = (MatchEventDetails.CardDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    cDto.getCardType().equals(MatchEventDetails.CardDto.CardType.DIRECT_RED);
        }));
    }

    @Test
    @DisplayName("processEvent rejects a CARD event if a player with two yellow cards gets a third yellow card")
    public void processEvent_PlayerWithTwoYellowGetsThirdYellow_Throws() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // make sure that the player plays for a team from the match
        var testTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var testTeamPlayerId = testTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.CardDto("1", testTeamPlayerId.toString(), false);
        // put the player in the starting home lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(testTeamPlayerId).build()),
                List.of(),
                List.of(),
                List.of()
        );
        // create a past event with a yellow card for the player
        List<MatchEvent> pastEvents = List.of(new MatchEvent(
                match,
                new MatchEventDetails.CardDto(
                        "45",
                        UUID.randomUUID(),
                        testTeamPlayer.getTeam().getId(),
                        MatchEventDetails.CardDto.CardType.SECOND_YELLOW,
                        new MatchEventDetails.SerializedPlayerInfo(testTeamPlayerId, null, null)
                )
        ));

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(testTeamPlayerId)).willReturn(testTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        assertEquals("the player is already ejected", message);
    }

    @Test
    @DisplayName("processEvent rejects a CARD event if a player with two yellow cards gets a direct red card")
    public void processEvent_PlayerWithTwoYellowGetsDirectRed_Throws() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // make sure that the player plays for a team from the match
        var testTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var testTeamPlayerId = testTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.CardDto("1", testTeamPlayerId.toString(), true);
        // put the player in the starting home lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(testTeamPlayerId).build()),
                List.of(),
                List.of(),
                List.of()
        );
        // create a past event with a yellow card for the player
        List<MatchEvent> pastEvents = List.of(new MatchEvent(
                match,
                new MatchEventDetails.CardDto(
                        "45",
                        UUID.randomUUID(),
                        testTeamPlayer.getTeam().getId(),
                        MatchEventDetails.CardDto.CardType.SECOND_YELLOW,
                        new MatchEventDetails.SerializedPlayerInfo(testTeamPlayerId, null, null)
                )
        ));

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(testTeamPlayerId)).willReturn(testTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        assertEquals("the player is already ejected", message);
    }

    @Test
    @DisplayName("processEvent rejects a CARD event if a player with a direct red card gets a yellow card")
    public void processEvent_PlayerWithDirectRedGetsYellow_Throws() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // make sure that the player plays for a team from the match
        var testTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var testTeamPlayerId = testTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.CardDto("1", testTeamPlayerId.toString(), false);
        // put the player in the starting home lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(testTeamPlayerId).build()),
                List.of(),
                List.of(),
                List.of()
        );
        // create a past event with a yellow card for the player
        List<MatchEvent> pastEvents = List.of(new MatchEvent(
                match,
                new MatchEventDetails.CardDto(
                        "45",
                        UUID.randomUUID(),
                        testTeamPlayer.getTeam().getId(),
                        MatchEventDetails.CardDto.CardType.DIRECT_RED,
                        new MatchEventDetails.SerializedPlayerInfo(testTeamPlayerId, null, null)
                )
        ));

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(testTeamPlayerId)).willReturn(testTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        assertEquals("the player is already ejected", message);
    }

    @Test
    @DisplayName("processEvent rejects a CARD event if a player with a direct red card gets a direct red card")
    public void processEvent_PlayerWithDirectRedGetsDirectRed_Throws() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // make sure that the player plays for a team from the match
        var testTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var testTeamPlayerId = testTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.CardDto("1", testTeamPlayerId.toString(), true);
        // put the player in the starting home lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(testTeamPlayerId).build()),
                List.of(),
                List.of(),
                List.of()
        );
        // create a past event with a yellow card for the player
        List<MatchEvent> pastEvents = List.of(new MatchEvent(
                match,
                new MatchEventDetails.CardDto(
                        "45",
                        UUID.randomUUID(),
                        testTeamPlayer.getTeam().getId(),
                        MatchEventDetails.CardDto.CardType.DIRECT_RED,
                        new MatchEventDetails.SerializedPlayerInfo(testTeamPlayerId, null, null)
                )
        ));

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(testTeamPlayerId)).willReturn(testTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        assertEquals("the player is already ejected", message);
    }

    @Test
    @DisplayName("processEvent rejects GOAL events if the ball in the match is not in play")
    public void processEvent_BallNotInPlay_RejectsInvalidGoal() throws ResourceNotFoundException {
        var ballNotInPlayStatuses = List.of(
                MatchStatus.NOT_STARTED, MatchStatus.HALF_TIME, MatchStatus.POSTPONED, MatchStatus.ABANDONED
        );
        var match = new Match();
        var matchId = match.getId();
        var testEvent = new InsertMatchEvent.GoalDto(
                "1", UUID.randomUUID().toString(), UUID.randomUUID().toString(), false
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);

        for (MatchStatus status: ballNotInPlayStatuses) {
            match.setStatus(status);

            // when
            String message = assertThrows(MatchEventInvalidException.class, () -> {
                matchEventService.processEvent(matchId, testEvent);
            }).getMessage();

            // then
            assertEquals("event cannot be processed when the ball is not in play", message);
        }
    }

    @Test
    @DisplayName("processEvent rejects GOAL events if the scoring player does not play for either team")
    public void processEvent_ScoringPlayerDoesNotPlayForTeams_RejectsInvalidGoal() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // make sure that the player plays for a team that is not in the match
        var scoringTeamPlayer = new TeamPlayer(TestTeam.builder().build(), new Player(), Position.GOALKEEPER, 1);
        var scoringTeamPlayerId = scoringTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.GoalDto("1", scoringTeamPlayerId.toString(), null, false);

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(scoringTeamPlayerId)).willReturn(scoringTeamPlayer);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s does not play for either team", scoringTeamPlayerId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects GOAL events if the scoring player plays for the team in the match but is not in the lineup")
    public void processEvent_ScoringPlayerNotInLineup_RejectsInvalidGoal() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // make sure that the player plays for a team from the match
        var scoringTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var scoringTeamPlayerId = scoringTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.GoalDto("1", scoringTeamPlayerId.toString(), null, false);
        // create an empty lineup which ensures that the player won't be in it
        var teamLineup = new LineupDto();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(scoringTeamPlayerId)).willReturn(scoringTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s is not placed in the lineup of this match", scoringTeamPlayerId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects a GOAL event if an own goal contains information about an assisting player")
    public void processEvent_OwnGoalContainsAssistingPlayer_RejectsInvalidGoal() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // make sure that the player plays for a team from the match
        var scoringTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var scoringTeamPlayerId = scoringTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.GoalDto(
                "1", scoringTeamPlayerId.toString(), UUID.randomUUID().toString(), true
        );

        // put the player in the starting home lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(scoringTeamPlayerId).build()),
                List.of(),
                List.of(),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(scoringTeamPlayerId)).willReturn(scoringTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        assertEquals("own goals cannot have a player assisting", message);
    }

    @Test
    @DisplayName("processEvent rejects GOAL events if the assisting player does not play for either team")
    public void processEvent_AssistingPlayerDoesNotPlayForTeams_RejectsInvalidGoal() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var scoringTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var scoringTeamPlayerId = scoringTeamPlayer.getId();
        // this player does not play for any of the teams that are in the match
        var assistingTeamPlayer = new TeamPlayer(TestTeam.builder().name("Team C").build(), new Player(), Position.GOALKEEPER, 1);
        var assistingTeamPlayerId = assistingTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.GoalDto(
                "1", scoringTeamPlayerId.toString(), assistingTeamPlayerId.toString(), false
        );

        // only have the scoring player in the starting home lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(scoringTeamPlayerId).build()),
                List.of(),
                List.of(),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                scoringTeamPlayerId, Optional.of(scoringTeamPlayer),
                assistingTeamPlayerId, Optional.of(assistingTeamPlayer)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s does not play for either team", assistingTeamPlayerId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects GOAL events if the scoring player plays for the team in the match but is not in the lineup")
    public void processEvent_AssistingPlayerNotInLineup_RejectsInvalidGoal() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match and is in the lineup
        var scoringTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var scoringTeamPlayerId = scoringTeamPlayer.getId();
        // this player plays for the same team but is not in the lineup
        var assistingTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.DEFENDER, 8);
        var assistingTeamPlayerId = assistingTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.GoalDto(
                "1", scoringTeamPlayerId.toString(), assistingTeamPlayerId.toString(), false
        );

        // only have the scoring player in the starting home lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(scoringTeamPlayerId).build()),
                List.of(),
                List.of(),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                scoringTeamPlayerId, Optional.of(scoringTeamPlayer),
                assistingTeamPlayerId, Optional.of(assistingTeamPlayer)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s is not placed in the lineup of this match", assistingTeamPlayerId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects GOAL events if the scoring and assisting players play for opposite teams")
    public void processEvent_ScoringAndAssistingPlayersPlayForOppositeTeams_RejectsInvalidGoal() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var scoringTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var scoringTeamPlayerId = scoringTeamPlayer.getId();
        // this player also plays in the match
        var assistingTeamPlayer = new TeamPlayer(match.getAwayTeam(), new Player(), Position.GOALKEEPER, 1);
        var assistingTeamPlayerId = assistingTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.GoalDto(
                "1", scoringTeamPlayerId.toString(), assistingTeamPlayerId.toString(), false
        );

        // have both players in the lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(scoringTeamPlayerId).build()),
                List.of(),
                List.of(TestTeamPlayerDto.builder().id(assistingTeamPlayerId).build()),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                scoringTeamPlayerId, Optional.of(scoringTeamPlayer),
                assistingTeamPlayerId, Optional.of(assistingTeamPlayer)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        assertEquals("players do not play for the same team", message);
    }

    @Test
    @DisplayName("processEvent accepts home GOAL events happening in the FIRST_HALF and correctly increments the scorelines")
    public void processEvent_FirstHalfHomeGoal_CorrectlyIncrementsScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var scoringTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var scoringTeamPlayerId = scoringTeamPlayer.getId();
        // this player also plays in the match
        var assistingTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.DEFENDER, 9);
        var assistingTeamPlayerId = assistingTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.GoalDto(
                "1", scoringTeamPlayerId.toString(), assistingTeamPlayerId.toString(), false
        );

        // have both players in the lineup
        var teamLineup = new LineupDto(
                List.of(
                        TestTeamPlayerDto.builder().id(scoringTeamPlayerId).build(),
                        TestTeamPlayerDto.builder().id(assistingTeamPlayerId).build()
                ),
                List.of(),
                List.of(),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                scoringTeamPlayerId, Optional.of(scoringTeamPlayer),
                assistingTeamPlayerId, Optional.of(assistingTeamPlayer)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        // the main score should be 1-0
        assertEquals(1, match.getScoreInfo().getHomeGoals());
        assertEquals(0, match.getScoreInfo().getAwayGoals());
        // the half-time score should also be 1-0
        assertEquals(1, match.getHalfTimeScoreInfo().getHomeGoals());
        assertEquals(0, match.getHalfTimeScoreInfo().getAwayGoals());
        // the penalty score should remain untouched
        assertEquals(0, match.getPenaltiesInfo().getHomeGoals());
        assertEquals(0, match.getPenaltiesInfo().getAwayGoals());

        verify(matchEventRepository).save(argThat(matchEvent -> {
            MatchEventDetails.GoalDto gDto = (MatchEventDetails.GoalDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    gDto.getScoringPlayer().getTeamPlayerId().equals(scoringTeamPlayerId) &&
                    gDto.getAssistingPlayer().getTeamPlayerId().equals(assistingTeamPlayerId) &&
                    gDto.getTeamId().equals(match.getHomeTeam().getId()) &&
                    !gDto.isOwnGoal();
        }));
    }

    @Test
    @DisplayName("processEvent accepts home own GOAL events happening in the FIRST_HALF and correctly increments the scorelines")
    public void processEvent_FirstHalfHomeOwnGoal_CorrectlyIncrementsScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var scoringTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var scoringTeamPlayerId = scoringTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.GoalDto("1", scoringTeamPlayerId.toString(), null, true);

        // only have the scoring player in the lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(scoringTeamPlayerId).build()),
                List.of(),
                List.of(),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(scoringTeamPlayerId)).willReturn(scoringTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        // the main score should be 0-1
        assertEquals(0, match.getScoreInfo().getHomeGoals());
        assertEquals(1, match.getScoreInfo().getAwayGoals());
        // the half-time score should also be 0-1
        assertEquals(0, match.getHalfTimeScoreInfo().getHomeGoals());
        assertEquals(1, match.getHalfTimeScoreInfo().getAwayGoals());
        // the penalty score should remain untouched
        assertEquals(0, match.getPenaltiesInfo().getHomeGoals());
        assertEquals(0, match.getPenaltiesInfo().getAwayGoals());

        verify(matchEventRepository).save(argThat(matchEvent -> {
            MatchEventDetails.GoalDto gDto = (MatchEventDetails.GoalDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    gDto.getScoringPlayer().getTeamPlayerId().equals(scoringTeamPlayerId) &&
                    gDto.getAssistingPlayer() == null &&
                    gDto.getTeamId().equals(match.getAwayTeam().getId()) &&
                    gDto.isOwnGoal();
        }));
    }

    @Test
    @DisplayName("processEvent accepts away GOAL events happening in the FIRST_HALF and correctly increments the scorelines")
    public void processEvent_FirstHalfAwayGoal_CorrectlyIncrementsScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var scoringTeamPlayer = new TeamPlayer(match.getAwayTeam(), new Player(), Position.GOALKEEPER, 1);
        var scoringTeamPlayerId = scoringTeamPlayer.getId();
        // this player also plays in the match
        var assistingTeamPlayer = new TeamPlayer(match.getAwayTeam(), new Player(), Position.DEFENDER, 9);
        var assistingTeamPlayerId = assistingTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.GoalDto(
                "1", scoringTeamPlayerId.toString(), assistingTeamPlayerId.toString(), false
        );

        // have both players in the lineup
        var teamLineup = new LineupDto(
                List.of(),
                List.of(),
                List.of(
                        TestTeamPlayerDto.builder().id(scoringTeamPlayerId).build(),
                        TestTeamPlayerDto.builder().id(assistingTeamPlayerId).build()
                ),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                scoringTeamPlayerId, Optional.of(scoringTeamPlayer),
                assistingTeamPlayerId, Optional.of(assistingTeamPlayer)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        // the main score should be 0-1
        assertEquals(0, match.getScoreInfo().getHomeGoals());
        assertEquals(1, match.getScoreInfo().getAwayGoals());
        // the half-time score should also be 0-1
        assertEquals(0, match.getHalfTimeScoreInfo().getHomeGoals());
        assertEquals(1, match.getHalfTimeScoreInfo().getAwayGoals());
        // the penalty score should remain untouched
        assertEquals(0, match.getPenaltiesInfo().getHomeGoals());
        assertEquals(0, match.getPenaltiesInfo().getAwayGoals());

        verify(matchEventRepository).save(argThat(matchEvent -> {
            MatchEventDetails.GoalDto gDto = (MatchEventDetails.GoalDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    gDto.getScoringPlayer().getTeamPlayerId().equals(scoringTeamPlayerId) &&
                    gDto.getAssistingPlayer().getTeamPlayerId().equals(assistingTeamPlayerId) &&
                    gDto.getTeamId().equals(match.getAwayTeam().getId()) &&
                    !gDto.isOwnGoal();
        }));
    }

    @Test
    @DisplayName("processEvent accepts away own GOAL events happening in the FIRST_HALF and correctly increments the scorelines")
    public void processEvent_FirstHalfAwayOwnGoal_CorrectlyIncrementsScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var scoringTeamPlayer = new TeamPlayer(match.getAwayTeam(), new Player(), Position.GOALKEEPER, 1);
        var scoringTeamPlayerId = scoringTeamPlayer.getId();

        var testEvent = new InsertMatchEvent.GoalDto("1", scoringTeamPlayerId.toString(), null, true);

        // only have the scoring player in the lineup
        var teamLineup = new LineupDto(
                List.of(),
                List.of(),
                List.of(TestTeamPlayerDto.builder().id(scoringTeamPlayerId).build()),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(scoringTeamPlayerId)).willReturn(scoringTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        // the main score should be 1-0
        assertEquals(1, match.getScoreInfo().getHomeGoals());
        assertEquals(0, match.getScoreInfo().getAwayGoals());
        // the half-time score should also be 1-0
        assertEquals(1, match.getHalfTimeScoreInfo().getHomeGoals());
        assertEquals(0, match.getHalfTimeScoreInfo().getAwayGoals());
        // the penalty score should remain untouched
        assertEquals(0, match.getPenaltiesInfo().getHomeGoals());
        assertEquals(0, match.getPenaltiesInfo().getAwayGoals());

        verify(matchEventRepository).save(argThat(matchEvent -> {
            MatchEventDetails.GoalDto gDto = (MatchEventDetails.GoalDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    gDto.getScoringPlayer().getTeamPlayerId().equals(scoringTeamPlayerId) &&
                    gDto.getAssistingPlayer() == null &&
                    gDto.getTeamId().equals(match.getHomeTeam().getId()) &&
                    gDto.isOwnGoal();
        }));
    }

    @Test
    @DisplayName("processEvent accepts home GOAL events happening outside the FIRST_HALF and correctly increments the scorelines")
    public void processEvent_NonFirstHalfHomeGoal_CorrectlyIncrementsScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var statuses = List.of(MatchStatus.SECOND_HALF, MatchStatus.EXTRA_TIME);

        for (MatchStatus status : statuses) {
            var match = TestMatch.builder().status(status).build();
            var matchId = match.getId();

            // this player plays in the match
            var scoringTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
            var scoringTeamPlayerId = scoringTeamPlayer.getId();
            // this player also plays in the match
            var assistingTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.DEFENDER, 9);
            var assistingTeamPlayerId = assistingTeamPlayer.getId();

            var testEvent = new InsertMatchEvent.GoalDto(
                    "1", scoringTeamPlayerId.toString(), assistingTeamPlayerId.toString(), false
            );

            // have both players in the lineup
            var teamLineup = new LineupDto(
                    List.of(
                            TestTeamPlayerDto.builder().id(scoringTeamPlayerId).build(),
                            TestTeamPlayerDto.builder().id(assistingTeamPlayerId).build()
                    ),
                    List.of(),
                    List.of(),
                    List.of()
            );

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            bindTeamPlayerIdsToTeamPlayers(Map.of(
                    scoringTeamPlayerId, Optional.of(scoringTeamPlayer),
                    assistingTeamPlayerId, Optional.of(assistingTeamPlayer)
            ));
            given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

            // when
            matchEventService.processEvent(matchId, testEvent);

            // then
            // the main score should be 1-0
            assertEquals(1, match.getScoreInfo().getHomeGoals());
            assertEquals(0, match.getScoreInfo().getAwayGoals());
            // the half-time score should remain untouched
            assertEquals(0, match.getHalfTimeScoreInfo().getHomeGoals());
            assertEquals(0, match.getHalfTimeScoreInfo().getAwayGoals());
            // the penalty score should remain untouched
            assertEquals(0, match.getPenaltiesInfo().getHomeGoals());
            assertEquals(0, match.getPenaltiesInfo().getAwayGoals());

            verify(matchEventRepository).save(argThat(matchEvent -> {
                MatchEventDetails.GoalDto gDto = (MatchEventDetails.GoalDto) matchEvent.getEvent();
                return matchEvent.getMatch().getId().equals(matchId) &&
                        gDto.getScoringPlayer().getTeamPlayerId().equals(scoringTeamPlayerId) &&
                        gDto.getAssistingPlayer().getTeamPlayerId().equals(assistingTeamPlayerId) &&
                        gDto.getTeamId().equals(match.getHomeTeam().getId()) &&
                        !gDto.isOwnGoal();
            }));
        }
    }

    @Test
    @DisplayName("processEvent accepts home own GOAL events happening outside the FIRST_HALF and correctly increments the scorelines")
    public void processEvent_NonFirstHalfHomeOwnGoal_CorrectlyIncrementsScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var statuses = List.of(MatchStatus.SECOND_HALF, MatchStatus.EXTRA_TIME);

        for (MatchStatus status : statuses) {
            var match = TestMatch.builder().status(status).build();
            var matchId = match.getId();

            // this player plays in the match
            var scoringTeamPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
            var scoringTeamPlayerId = scoringTeamPlayer.getId();

            var testEvent = new InsertMatchEvent.GoalDto("1", scoringTeamPlayerId.toString(), null, true);

            // only have the scoring player in the lineup
            var teamLineup = new LineupDto(
                    List.of(TestTeamPlayerDto.builder().id(scoringTeamPlayerId).build()),
                    List.of(),
                    List.of(),
                    List.of()
            );

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            given(teamPlayerService.findEntityById(scoringTeamPlayerId)).willReturn(scoringTeamPlayer);
            given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

            // when
            matchEventService.processEvent(matchId, testEvent);

            // then
            // the main score should be 0-1
            assertEquals(0, match.getScoreInfo().getHomeGoals());
            assertEquals(1, match.getScoreInfo().getAwayGoals());
            // the half-time score should remain untouched
            assertEquals(0, match.getHalfTimeScoreInfo().getHomeGoals());
            assertEquals(0, match.getHalfTimeScoreInfo().getAwayGoals());
            // the penalty score should remain untouched
            assertEquals(0, match.getPenaltiesInfo().getHomeGoals());
            assertEquals(0, match.getPenaltiesInfo().getAwayGoals());

            verify(matchEventRepository).save(argThat(matchEvent -> {
                MatchEventDetails.GoalDto gDto = (MatchEventDetails.GoalDto) matchEvent.getEvent();
                return matchEvent.getMatch().getId().equals(matchId) &&
                        gDto.getScoringPlayer().getTeamPlayerId().equals(scoringTeamPlayerId) &&
                        gDto.getAssistingPlayer() == null &&
                        gDto.getTeamId().equals(match.getAwayTeam().getId()) &&
                        gDto.isOwnGoal();
            }));
        }
    }

    @Test
    @DisplayName("processEvent accepts away GOAL events happening outside the FIRST_HALF and correctly increments the scorelines")
    public void processEvent_NonFirstHalfAwayGoal_CorrectlyIncrementsScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var statuses = List.of(MatchStatus.SECOND_HALF, MatchStatus.EXTRA_TIME);

        for (MatchStatus status : statuses) {
            var match = TestMatch.builder().status(status).build();
            var matchId = match.getId();

            // this player plays in the match
            var scoringTeamPlayer = new TeamPlayer(match.getAwayTeam(), new Player(), Position.GOALKEEPER, 1);
            var scoringTeamPlayerId = scoringTeamPlayer.getId();
            // this player also plays in the match
            var assistingTeamPlayer = new TeamPlayer(match.getAwayTeam(), new Player(), Position.DEFENDER, 9);
            var assistingTeamPlayerId = assistingTeamPlayer.getId();

            var testEvent = new InsertMatchEvent.GoalDto(
                    "1", scoringTeamPlayerId.toString(), assistingTeamPlayerId.toString(), false
            );

            // have both players in the lineup
            var teamLineup = new LineupDto(
                    List.of(),
                    List.of(),
                    List.of(
                            TestTeamPlayerDto.builder().id(scoringTeamPlayerId).build(),
                            TestTeamPlayerDto.builder().id(assistingTeamPlayerId).build()
                    ),
                    List.of()
            );

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            bindTeamPlayerIdsToTeamPlayers(Map.of(
                    scoringTeamPlayerId, Optional.of(scoringTeamPlayer),
                    assistingTeamPlayerId, Optional.of(assistingTeamPlayer)
            ));
            given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

            // when
            matchEventService.processEvent(matchId, testEvent);

            // then
            // the main score should be 0-1
            assertEquals(0, match.getScoreInfo().getHomeGoals());
            assertEquals(1, match.getScoreInfo().getAwayGoals());
            // the half-time score should remain untouched
            assertEquals(0, match.getHalfTimeScoreInfo().getHomeGoals());
            assertEquals(0, match.getHalfTimeScoreInfo().getAwayGoals());
            // the penalty score should remain untouched
            assertEquals(0, match.getPenaltiesInfo().getHomeGoals());
            assertEquals(0, match.getPenaltiesInfo().getAwayGoals());

            verify(matchEventRepository).save(argThat(matchEvent -> {
                MatchEventDetails.GoalDto gDto = (MatchEventDetails.GoalDto) matchEvent.getEvent();
                return matchEvent.getMatch().getId().equals(matchId) &&
                        gDto.getScoringPlayer().getTeamPlayerId().equals(scoringTeamPlayerId) &&
                        gDto.getAssistingPlayer().getTeamPlayerId().equals(assistingTeamPlayerId) &&
                        gDto.getTeamId().equals(match.getAwayTeam().getId()) &&
                        !gDto.isOwnGoal();
            }));
        }
    }

    @Test
    @DisplayName("processEvent accepts away own GOAL events happening outside the FIRST_HALF and correctly increments the scorelines")
    public void processEvent_NonFirstHalfAwayOwnGoal_CorrectlyIncrementsScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var statuses = List.of(MatchStatus.SECOND_HALF, MatchStatus.EXTRA_TIME);

        for (MatchStatus status : statuses) {
            var match = TestMatch.builder().status(status).build();
            var matchId = match.getId();

            var scoringTeamPlayer = new TeamPlayer(match.getAwayTeam(), new Player(), Position.GOALKEEPER, 1);
            var scoringTeamPlayerId = scoringTeamPlayer.getId();

            var testEvent = new InsertMatchEvent.GoalDto("1", scoringTeamPlayerId.toString(), null, true);

            // only have the scoring player in the lineup
            var teamLineup = new LineupDto(
                    List.of(),
                    List.of(),
                    List.of(TestTeamPlayerDto.builder().id(scoringTeamPlayerId).build()),
                    List.of()
            );

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            given(teamPlayerService.findEntityById(scoringTeamPlayerId)).willReturn(scoringTeamPlayer);
            given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

            // when
            matchEventService.processEvent(matchId, testEvent);

            // then
            // the main score should be 1-0
            assertEquals(1, match.getScoreInfo().getHomeGoals());
            assertEquals(0, match.getScoreInfo().getAwayGoals());
            // the half-time score should remain untouched
            assertEquals(0, match.getHalfTimeScoreInfo().getHomeGoals());
            assertEquals(0, match.getHalfTimeScoreInfo().getAwayGoals());
            // the penalty score should remain untouched
            assertEquals(0, match.getPenaltiesInfo().getHomeGoals());
            assertEquals(0, match.getPenaltiesInfo().getAwayGoals());

            verify(matchEventRepository).save(argThat(matchEvent -> {
                MatchEventDetails.GoalDto gDto = (MatchEventDetails.GoalDto) matchEvent.getEvent();
                return matchEvent.getMatch().getId().equals(matchId) &&
                        gDto.getScoringPlayer().getTeamPlayerId().equals(scoringTeamPlayerId) &&
                        gDto.getAssistingPlayer() == null &&
                        gDto.getTeamId().equals(match.getHomeTeam().getId()) &&
                        gDto.isOwnGoal();
            }));
        }
    }
}
