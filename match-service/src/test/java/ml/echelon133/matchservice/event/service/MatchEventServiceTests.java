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

    private void assertEventInvalidWhenBallNotInPlay(Match testedMatch, InsertMatchEvent event) {
        var ballNotInPlay = List.of(
                MatchStatus.NOT_STARTED, MatchStatus.HALF_TIME,
                MatchStatus.POSTPONED, MatchStatus.ABANDONED,
                MatchStatus.FINISHED
        );
        for (MatchStatus status: ballNotInPlay) {
            testedMatch.setStatus(status);

            // when
            String message = assertThrows(MatchEventInvalidException.class, () -> {
                matchEventService.processEvent(testedMatch.getId(), event);
            }).getMessage();

            // then
            assertEquals("event cannot be processed when the ball is not in play", message);
        }
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
    @DisplayName("findAllByMatchId returns expected event dtos")
    public void findAllByMatchId_HasStatusEvent_ReturnsDto() {
        var match = TestMatch.builder().build();
        var matchId = match.getId();
        var targetStatus = MatchStatus.FIRST_HALF;
        var statusEvent = new MatchEvent(
                match, new MatchEventDetails.StatusDto("1", match.getCompetitionId(), targetStatus)
        );

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

        var match = TestMatch.builder().build();
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
        var match = TestMatch.builder().build();
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
        var match = TestMatch.builder().build();
        var matchId = match.getId();
        var testEvent = new InsertMatchEvent.CardDto("1", UUID.randomUUID().toString(), false);

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);

        // then
        assertEventInvalidWhenBallNotInPlay(match, testEvent);
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
        // create a past event with a two yellow cards for the player
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
        // create a past event with two yellow cards for the player
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
        // create a past event with a direct red card for the player
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
        // create a past event with a direct red card for the player
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
        var match = TestMatch.builder().build();
        var matchId = match.getId();
        var testEvent = new InsertMatchEvent.GoalDto(
                "1", UUID.randomUUID().toString(), UUID.randomUUID().toString(), false
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);

        // then
        assertEventInvalidWhenBallNotInPlay(match, testEvent);
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

    @Test
    @DisplayName("processEvent rejects SUBSTITUTION events if the ball in the match is not in play")
    public void processEvent_BallNotInPlay_RejectsInvalidSubstitution() throws ResourceNotFoundException {
        var match = TestMatch.builder().build();
        var matchId = match.getId();
        var testEvent = new InsertMatchEvent.SubstitutionDto(
                "1", UUID.randomUUID().toString(), UUID.randomUUID().toString()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);

        // then
        assertEventInvalidWhenBallNotInPlay(match, testEvent);
    }

    @Test
    @DisplayName("processEvent rejects SUBSTITUTION events if the playerIn does not play for either team")
    public void processEvent_PlayerInDoesNotPlayForTeams_RejectsInvalidSubstitution() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // make sure that the player plays for a team that is not in the match
        var teamPlayerIn = new TeamPlayer(TestTeam.builder().build(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerInId = teamPlayerIn.getId();

        var testEvent = new InsertMatchEvent.SubstitutionDto("1", teamPlayerInId.toString(), UUID.randomUUID().toString());

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(teamPlayerInId)).willReturn(teamPlayerIn);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s does not play for either team", teamPlayerInId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects SUBSTITUTION events if the playerIn plays for the team in the match but is not in the lineup")
    public void processEvent_PlayerInNotInLineup_RejectsInvalidSubstitution() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // make sure that the player plays for a team from the match
        var teamPlayerIn = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerInId = teamPlayerIn.getId();

        var testEvent = new InsertMatchEvent.SubstitutionDto("1", teamPlayerInId.toString(), null);
        // create an empty lineup which ensures that the player won't be in it
        var teamLineup = new LineupDto();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(teamPlayerInId)).willReturn(teamPlayerIn);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s is not placed in the lineup of this match", teamPlayerInId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects SUBSTITUTION events if the playerOut does not play for either team")
    public void processEvent_PlayerOutDoesNotPlayForTeams_RejectsInvalidSubstitution() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var teamPlayerIn = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerInId = teamPlayerIn.getId();
        // this player does not play for any of the teams that are in the match
        var teamPlayerOut = new TeamPlayer(TestTeam.builder().name("Team C").build(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerOutId = teamPlayerOut.getId();

        var testEvent = new InsertMatchEvent.SubstitutionDto("1", teamPlayerInId.toString(), teamPlayerOutId.toString());

        // only have the playerIn in the starting home lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(teamPlayerInId).build()),
                List.of(),
                List.of(),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                teamPlayerInId, Optional.of(teamPlayerIn),
                teamPlayerOutId, Optional.of(teamPlayerOut)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s does not play for either team", teamPlayerOutId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects SUBSTITUTION events if the playerOut plays for the team in the match but is not in the lineup")
    public void processEvent_PlayerOutNotInLineup_RejectsInvalidSubstitution() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match and is in the lineup
        var teamPlayerIn = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerInId = teamPlayerIn.getId();
        // this player plays for the same team but is not in the lineup
        var teamPlayerOut = new TeamPlayer(match.getHomeTeam(), new Player(), Position.DEFENDER, 8);
        var teamPlayerOutId = teamPlayerOut.getId();

        var testEvent = new InsertMatchEvent.SubstitutionDto("1", teamPlayerInId.toString(), teamPlayerOutId.toString());

        // only have the playerIn in the starting home lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(teamPlayerInId).build()),
                List.of(),
                List.of(),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                teamPlayerInId, Optional.of(teamPlayerIn),
                teamPlayerOutId, Optional.of(teamPlayerOut)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s is not placed in the lineup of this match", teamPlayerOutId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects SUBSTITUTION events if the playerIn and playerOut play for opposite teams")
    public void processEvent_PlayerInAndPlayerOutPlayForOppositeTeams_RejectsInvalidSubstitution() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var teamPlayerIn = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerInId = teamPlayerIn.getId();
        // this player also plays in the match
        var teamPlayerOut = new TeamPlayer(match.getAwayTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerOutId = teamPlayerOut.getId();

        var testEvent = new InsertMatchEvent.SubstitutionDto("1", teamPlayerInId.toString(), teamPlayerOutId.toString());

        // have both players in the lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(teamPlayerInId).build()),
                List.of(),
                List.of(TestTeamPlayerDto.builder().id(teamPlayerOutId).build()),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                teamPlayerInId, Optional.of(teamPlayerIn),
                teamPlayerOutId, Optional.of(teamPlayerOut)
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
    @DisplayName("processEvent rejects SUBSTITUTION events if the playerIn did not start the game on the bench")
    public void processEvent_PlayerInDidNotStartOnBench_RejectsInvalidSubstitution() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var teamPlayerIn = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerInId = teamPlayerIn.getId();
        // this player also plays in the match
        var teamPlayerOut = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerOutId = teamPlayerOut.getId();

        var testEvent = new InsertMatchEvent.SubstitutionDto("1", teamPlayerInId.toString(), teamPlayerOutId.toString());

        // have both players in the lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(teamPlayerInId).build(),
                        TestTeamPlayerDto.builder().id(teamPlayerOutId).build()),
                List.of(),
                List.of(),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                teamPlayerInId, Optional.of(teamPlayerIn),
                teamPlayerOutId, Optional.of(teamPlayerOut)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s cannot enter the pitch", teamPlayerInId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects SUBSTITUTION events if the playerIn got two yellow cards")
    public void processEvent_PlayerInHasTwoYellowCards_RejectsInvalidSubstitution() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var teamPlayerIn = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerInId = teamPlayerIn.getId();
        // this player also plays in the match
        var teamPlayerOut = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerOutId = teamPlayerOut.getId();

        var testEvent = new InsertMatchEvent.SubstitutionDto("1", teamPlayerInId.toString(), teamPlayerOutId.toString());

        // have both players in the lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(teamPlayerOutId).build()),
                List.of(TestTeamPlayerDto.builder().id(teamPlayerInId).build()),
                List.of(),
                List.of()
        );

        // create a past event with two yellow cards for the player
        List<MatchEvent> pastEvents = List.of(new MatchEvent(
                match,
                new MatchEventDetails.CardDto(
                        "45",
                        UUID.randomUUID(),
                        teamPlayerIn.getTeam().getId(),
                        MatchEventDetails.CardDto.CardType.SECOND_YELLOW,
                        new MatchEventDetails.SerializedPlayerInfo(teamPlayerInId, null, null)
                )
        ));

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                teamPlayerInId, Optional.of(teamPlayerIn),
                teamPlayerOutId, Optional.of(teamPlayerOut)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s cannot enter the pitch", teamPlayerInId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects SUBSTITUTION events if the playerIn got a red card")
    public void processEvent_PlayerInHasRedCard_RejectsInvalidSubstitution() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var teamPlayerIn = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerInId = teamPlayerIn.getId();
        // this player also plays in the match
        var teamPlayerOut = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerOutId = teamPlayerOut.getId();

        var testEvent = new InsertMatchEvent.SubstitutionDto("1", teamPlayerInId.toString(), teamPlayerOutId.toString());

        // have both players in the lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(teamPlayerOutId).build()),
                List.of(TestTeamPlayerDto.builder().id(teamPlayerInId).build()),
                List.of(),
                List.of()
        );

        // create a past event with a direct red card for the player
        List<MatchEvent> pastEvents = List.of(new MatchEvent(
                match,
                new MatchEventDetails.CardDto(
                        "45",
                        UUID.randomUUID(),
                        teamPlayerIn.getTeam().getId(),
                        MatchEventDetails.CardDto.CardType.DIRECT_RED,
                        new MatchEventDetails.SerializedPlayerInfo(teamPlayerInId, null, null)
                )
        ));

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                teamPlayerInId, Optional.of(teamPlayerIn),
                teamPlayerOutId, Optional.of(teamPlayerOut)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s cannot enter the pitch", teamPlayerInId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects SUBSTITUTION events if the playerIn got already subbed on before")
    public void processEvent_PlayerInAlreadySubbedOn_RejectsInvalidSubstitution() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var teamPlayerIn = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerInId = teamPlayerIn.getId();
        // this player also plays in the match
        var teamPlayerOut = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerOutId = teamPlayerOut.getId();

        var testEvent = new InsertMatchEvent.SubstitutionDto("1", teamPlayerInId.toString(), teamPlayerOutId.toString());

        // have both players in the lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(teamPlayerOutId).build()),
                List.of(TestTeamPlayerDto.builder().id(teamPlayerInId).build()),
                List.of(),
                List.of()
        );

        // create a past event where the playerIn already got subbed on before
        List<MatchEvent> pastEvents = List.of(new MatchEvent(
                match,
                new MatchEventDetails.SubstitutionDto(
                        "45",
                        UUID.randomUUID(),
                        teamPlayerIn.getTeam().getId(),
                        new MatchEventDetails.SerializedPlayerInfo(teamPlayerInId, null, null),
                        new MatchEventDetails.SerializedPlayerInfo(teamPlayerOutId, null, null)
                )
        ));

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                teamPlayerInId, Optional.of(teamPlayerIn),
                teamPlayerOutId, Optional.of(teamPlayerOut)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s cannot enter the pitch", teamPlayerInId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects SUBSTITUTION events if the playerOut got two yellow cards")
    public void processEvent_PlayerOutHasTwoYellowCards_RejectsInvalidSubstitution() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var teamPlayerIn = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerInId = teamPlayerIn.getId();
        // this player also plays in the match
        var teamPlayerOut = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerOutId = teamPlayerOut.getId();

        var testEvent = new InsertMatchEvent.SubstitutionDto("1", teamPlayerInId.toString(), teamPlayerOutId.toString());

        // have both players in the lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(teamPlayerOutId).build()),
                List.of(TestTeamPlayerDto.builder().id(teamPlayerInId).build()),
                List.of(),
                List.of()
        );

        // create a past event with two yellow cards for the player
        List<MatchEvent> pastEvents = List.of(new MatchEvent(
                match,
                new MatchEventDetails.CardDto(
                        "45",
                        UUID.randomUUID(),
                        teamPlayerIn.getTeam().getId(),
                        MatchEventDetails.CardDto.CardType.SECOND_YELLOW,
                        new MatchEventDetails.SerializedPlayerInfo(teamPlayerOutId, null, null)
                )
        ));

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                teamPlayerInId, Optional.of(teamPlayerIn),
                teamPlayerOutId, Optional.of(teamPlayerOut)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s is not on the pitch", teamPlayerOutId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects SUBSTITUTION events if the playerOut got a red card")
    public void processEvent_PlayerOutHasRedCard_RejectsInvalidSubstitution() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var teamPlayerIn = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerInId = teamPlayerIn.getId();
        // this player also plays in the match
        var teamPlayerOut = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerOutId = teamPlayerOut.getId();

        var testEvent = new InsertMatchEvent.SubstitutionDto("1", teamPlayerInId.toString(), teamPlayerOutId.toString());

        // have both players in the lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(teamPlayerOutId).build()),
                List.of(TestTeamPlayerDto.builder().id(teamPlayerInId).build()),
                List.of(),
                List.of()
        );

        // create a past event with a direct red card for the player
        List<MatchEvent> pastEvents = List.of(new MatchEvent(
                match,
                new MatchEventDetails.CardDto(
                        "45",
                        UUID.randomUUID(),
                        teamPlayerIn.getTeam().getId(),
                        MatchEventDetails.CardDto.CardType.DIRECT_RED,
                        new MatchEventDetails.SerializedPlayerInfo(teamPlayerOutId, null, null)
                )
        ));

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                teamPlayerInId, Optional.of(teamPlayerIn),
                teamPlayerOutId, Optional.of(teamPlayerOut)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s is not on the pitch", teamPlayerOutId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects SUBSTITUTION events if the playerOut got already subbed off")
    public void processEvent_PlayerOutAlreadySubbedOff_RejectsInvalidSubstitution() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var teamPlayerIn = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerInId = teamPlayerIn.getId();
        // this player also plays in the match
        var teamPlayerOut = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerOutId = teamPlayerOut.getId();

        var testEvent = new InsertMatchEvent.SubstitutionDto("1", teamPlayerInId.toString(), teamPlayerOutId.toString());

        // have both players in the lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(teamPlayerOutId).build()),
                List.of(TestTeamPlayerDto.builder().id(teamPlayerInId).build()),
                List.of(),
                List.of()
        );

        // create a past event where the playerOut already got subbed off before
        List<MatchEvent> pastEvents = List.of(new MatchEvent(
                match,
                new MatchEventDetails.SubstitutionDto(
                        "45",
                        UUID.randomUUID(),
                        teamPlayerIn.getTeam().getId(),
                        new MatchEventDetails.SerializedPlayerInfo(UUID.randomUUID(), null, null),
                        new MatchEventDetails.SerializedPlayerInfo(teamPlayerOutId, null, null)
                )
        ));

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                teamPlayerInId, Optional.of(teamPlayerIn),
                teamPlayerOutId, Optional.of(teamPlayerOut)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s is not on the pitch", teamPlayerOutId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects SUBSTITUTION events if the playerOut spent the entire match on the bench")
    public void processEvent_PlayerOutConstantlyOnBench_RejectsInvalidSubstitution() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var teamPlayerIn = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerInId = teamPlayerIn.getId();
        // this player also plays in the match
        var teamPlayerOut = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerOutId = teamPlayerOut.getId();

        var testEvent = new InsertMatchEvent.SubstitutionDto("1", teamPlayerInId.toString(), teamPlayerOutId.toString());

        // have both players in the lineup
        var teamLineup = new LineupDto(
                List.of(),
                List.of(
                        TestTeamPlayerDto.builder().id(teamPlayerInId).build(),
                        TestTeamPlayerDto.builder().id(teamPlayerOutId).build()
                ),
                List.of(),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                teamPlayerInId, Optional.of(teamPlayerIn),
                teamPlayerOutId, Optional.of(teamPlayerOut)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s is not on the pitch", teamPlayerOutId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent accepts SUBSTITUTION events if neither player has been carded")
    public void processEvent_CorrectPlayerInAndPlayerOutWithoutCards_SavesSubstitution() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var teamPlayerIn = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerInId = teamPlayerIn.getId();
        // this player also plays in the match
        var teamPlayerOut = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerOutId = teamPlayerOut.getId();

        var testEvent = new InsertMatchEvent.SubstitutionDto("1", teamPlayerInId.toString(), teamPlayerOutId.toString());

        // have both players in the lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(teamPlayerOutId).build()),
                List.of(TestTeamPlayerDto.builder().id(teamPlayerInId).build()),
                List.of(),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                teamPlayerInId, Optional.of(teamPlayerIn),
                teamPlayerOutId, Optional.of(teamPlayerOut)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        verify(matchEventRepository).save(argThat(matchEvent -> {
            MatchEventDetails.SubstitutionDto sDto = (MatchEventDetails.SubstitutionDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    sDto.getTeamId().equals(match.getHomeTeam().getId()) &&
                    sDto.getPlayerIn().getTeamPlayerId().equals(teamPlayerInId) &&
                    sDto.getPlayerOut().getTeamPlayerId().equals(teamPlayerOutId);
        }));
    }

    @Test
    @DisplayName("processEvent accepts SUBSTITUTION events if both players have a yellow card each")
    public void processEvent_CorrectPlayerInAndPlayerOutWithYellowCards_SavesSubstitution() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var teamPlayerIn = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerInId = teamPlayerIn.getId();
        // this player also plays in the match
        var teamPlayerOut = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerOutId = teamPlayerOut.getId();

        var testEvent = new InsertMatchEvent.SubstitutionDto("1", teamPlayerInId.toString(), teamPlayerOutId.toString());

        // have both players in the lineup
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(teamPlayerOutId).build()),
                List.of(TestTeamPlayerDto.builder().id(teamPlayerInId).build()),
                List.of(),
                List.of()
        );

        // give both players yellow cards
        List<MatchEvent> pastEvents = List.of(
                new MatchEvent(
                    match,
                    new MatchEventDetails.CardDto(
                            "45",
                            UUID.randomUUID(),
                            teamPlayerIn.getTeam().getId(),
                            MatchEventDetails.CardDto.CardType.YELLOW,
                            new MatchEventDetails.SerializedPlayerInfo(teamPlayerInId, null, null)
                    )),
                new MatchEvent(
                    match,
                    new MatchEventDetails.CardDto(
                            "45",
                            UUID.randomUUID(),
                            teamPlayerIn.getTeam().getId(),
                            MatchEventDetails.CardDto.CardType.YELLOW,
                            new MatchEventDetails.SerializedPlayerInfo(teamPlayerOutId, null, null)
                    ))
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                teamPlayerInId, Optional.of(teamPlayerIn),
                teamPlayerOutId, Optional.of(teamPlayerOut)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        verify(matchEventRepository).save(argThat(matchEvent -> {
            MatchEventDetails.SubstitutionDto sDto = (MatchEventDetails.SubstitutionDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    sDto.getTeamId().equals(match.getHomeTeam().getId()) &&
                    sDto.getPlayerIn().getTeamPlayerId().equals(teamPlayerInId) &&
                    sDto.getPlayerOut().getTeamPlayerId().equals(teamPlayerOutId);
        }));
    }

    @Test
    @DisplayName("processEvent accepts SUBSTITUTION events if the player getting subbed off got subbed on before")
    public void processEvent_SubstituteCanBeBenchedAgain_SavesSubstitution() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var teamPlayerIn = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerInId = teamPlayerIn.getId();
        // this player also plays in the match
        var teamPlayerOut = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var teamPlayerOutId = teamPlayerOut.getId();

        // assume that playerIn is already on the pitch because of a substitution
        var testEvent = new InsertMatchEvent.SubstitutionDto("1", teamPlayerInId.toString(), teamPlayerOutId.toString());

        // have both players in the lineup
        var teamLineup = new LineupDto(
                List.of(),
                List.of(
                        TestTeamPlayerDto.builder().id(teamPlayerInId).build(),
                        TestTeamPlayerDto.builder().id(teamPlayerOutId).build()
                ),
                List.of(),
                List.of()
        );

        // create a past event where the playerOut gets subbed on
        List<MatchEvent> pastEvents = List.of(new MatchEvent(
                match,
                new MatchEventDetails.SubstitutionDto(
                        "45",
                        UUID.randomUUID(),
                        teamPlayerIn.getTeam().getId(),
                        new MatchEventDetails.SerializedPlayerInfo(teamPlayerOutId, null, null),
                        new MatchEventDetails.SerializedPlayerInfo(UUID.randomUUID(), null, null)
                )
        ));

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                teamPlayerInId, Optional.of(teamPlayerIn),
                teamPlayerOutId, Optional.of(teamPlayerOut)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        verify(matchEventRepository).save(argThat(matchEvent -> {
            MatchEventDetails.SubstitutionDto sDto = (MatchEventDetails.SubstitutionDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    sDto.getTeamId().equals(match.getHomeTeam().getId()) &&
                    sDto.getPlayerIn().getTeamPlayerId().equals(teamPlayerInId) &&
                    sDto.getPlayerOut().getTeamPlayerId().equals(teamPlayerOutId);
        }));
    }

    @Test
    @DisplayName("processEvent rejects PENALTY events if the ball in the match is not in play")
    public void processEvent_BallNotInPlay_RejectsInvalidPenalty() throws ResourceNotFoundException {
        var match = TestMatch.builder().build();
        var matchId = match.getId();
        var testEvent = new InsertMatchEvent.PenaltyDto("1", UUID.randomUUID().toString(), true);

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);

        // then
        assertEventInvalidWhenBallNotInPlay(match, testEvent);
    }

    @Test
    @DisplayName("processEvent rejects PENALTY events if the shootingPlayer got two yellow cards")
    public void processEvent_ShootingPlayerHasTwoYellowCards_RejectsInvalidPenalty() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var shootingPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var shootingPlayerId = shootingPlayer.getId();

        var testEvent = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), true);

        // create a past event with two yellow cards for the player
        List<MatchEvent> pastEvents = List.of(new MatchEvent(
                match,
                new MatchEventDetails.CardDto(
                        "45",
                        UUID.randomUUID(),
                        shootingPlayer.getTeam().getId(),
                        MatchEventDetails.CardDto.CardType.SECOND_YELLOW,
                        new MatchEventDetails.SerializedPlayerInfo(shootingPlayerId, null, null)
                )
        ));

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s is not on the pitch", shootingPlayerId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects PENALTY events if the shootingPlayer got a red card")
    public void processEvent_ShootingPlayerHasRedCard_RejectsInvalidPenalty() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var shootingPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var shootingPlayerId = shootingPlayer.getId();

        var testEvent = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), true);

        // create a past event with a red card for the player
        List<MatchEvent> pastEvents = List.of(new MatchEvent(
                match,
                new MatchEventDetails.CardDto(
                        "45",
                        UUID.randomUUID(),
                        shootingPlayer.getTeam().getId(),
                        MatchEventDetails.CardDto.CardType.DIRECT_RED,
                        new MatchEventDetails.SerializedPlayerInfo(shootingPlayerId, null, null)
                )
        ));

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s is not on the pitch", shootingPlayerId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects PENALTY events if the shooting player got already subbed off")
    public void processEvent_PlayerOutAlreadySubbedOff_RejectsInvalidPenalty() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        var shootingPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var shootingPlayerId = shootingPlayer.getId();

        var testEvent = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), true);

        // create a past event where the shootingPlayer already got subbed off before
        List<MatchEvent> pastEvents = List.of(new MatchEvent(
                match,
                new MatchEventDetails.SubstitutionDto(
                        "45",
                        UUID.randomUUID(),
                        shootingPlayer.getTeam().getId(),
                        new MatchEventDetails.SerializedPlayerInfo(UUID.randomUUID(), null, null),
                        new MatchEventDetails.SerializedPlayerInfo(shootingPlayerId, null, null)
                )
        ));

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(pastEvents);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s is not on the pitch", shootingPlayerId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent rejects PENALTY events if the shootingPlayer spent the entire match on the bench")
    public void processEvent_ShootingPlayerConstantlyOnBench_RejectsInvalidPenalty() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        var shootingPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var shootingPlayerId = shootingPlayer.getId();

        var testEvent = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), true);

        // have the shootingPlayer on the bench
        var teamLineup = new LineupDto(
                List.of(),
                List.of(TestTeamPlayerDto.builder().id(shootingPlayerId).build()),
                List.of(),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        var expectedMessage = String.format("the player %s is not on the pitch", shootingPlayerId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("processEvent accepts home PENALTY events happening in the FIRST_HALF and correctly increments the scorelines when the penalty is scored")
    public void processEvent_FirstHalfHomePenaltyGoal_CorrectlyIncrementsScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        var shootingPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var shootingPlayerId = shootingPlayer.getId();

        var testEvent = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), true);

        // have the shootingPlayer start on the pitch
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(shootingPlayerId).build()),
                List.of(),
                List.of(),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
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
            MatchEventDetails.PenaltyDto pDto = (MatchEventDetails.PenaltyDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    pDto.getTeamId().equals(match.getHomeTeam().getId()) &&
                    pDto.getShootingPlayer().getTeamPlayerId().equals(shootingPlayerId) &&
                    pDto.isCountAsGoal() &&
                    pDto.isScored();
        }));
    }

    @Test
    @DisplayName("processEvent accepts home PENALTY events happening in the FIRST_HALF and does not increment the scorelines when the penalty is missed")
    public void processEvent_FirstHalfHomePenaltyMissed_DoesNotTouchScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        var shootingPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var shootingPlayerId = shootingPlayer.getId();

        var testEvent = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), false);

        // have the shootingPlayer start on the pitch
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(shootingPlayerId).build()),
                List.of(),
                List.of(),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        // the main score should be 0-0
        assertEquals(0, match.getScoreInfo().getHomeGoals());
        assertEquals(0, match.getScoreInfo().getAwayGoals());
        // the half-time score should also be 0-0
        assertEquals(0, match.getHalfTimeScoreInfo().getHomeGoals());
        assertEquals(0, match.getHalfTimeScoreInfo().getAwayGoals());
        // the penalty score should remain untouched
        assertEquals(0, match.getPenaltiesInfo().getHomeGoals());
        assertEquals(0, match.getPenaltiesInfo().getAwayGoals());

        verify(matchEventRepository).save(argThat(matchEvent -> {
            MatchEventDetails.PenaltyDto pDto = (MatchEventDetails.PenaltyDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    pDto.getTeamId().equals(match.getHomeTeam().getId()) &&
                    pDto.getShootingPlayer().getTeamPlayerId().equals(shootingPlayerId) &&
                    pDto.isCountAsGoal() &&
                    !pDto.isScored();
        }));
    }

    @Test
    @DisplayName("processEvent accepts away PENALTY events happening in the FIRST_HALF and correctly increments the scorelines when the penalty is scored")
    public void processEvent_FirstHalfAwayPenaltyGoal_CorrectlyIncrementsScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        var shootingPlayer = new TeamPlayer(match.getAwayTeam(), new Player(), Position.GOALKEEPER, 1);
        var shootingPlayerId = shootingPlayer.getId();

        var testEvent = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), true);

        // have the shootingPlayer start on the pitch
        var teamLineup = new LineupDto(
                List.of(),
                List.of(),
                List.of(TestTeamPlayerDto.builder().id(shootingPlayerId).build()),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
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
            MatchEventDetails.PenaltyDto pDto = (MatchEventDetails.PenaltyDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    pDto.getTeamId().equals(match.getAwayTeam().getId()) &&
                    pDto.getShootingPlayer().getTeamPlayerId().equals(shootingPlayerId) &&
                    pDto.isCountAsGoal() &&
                    pDto.isScored();
        }));
    }

    @Test
    @DisplayName("processEvent accepts away PENALTY events happening in the FIRST_HALF and does not increment the scorelines when the penalty is missed")
    public void processEvent_FirstHalfAwayPenaltyMissed_DoesNotTouchScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        var shootingPlayer = new TeamPlayer(match.getAwayTeam(), new Player(), Position.GOALKEEPER, 1);
        var shootingPlayerId = shootingPlayer.getId();

        var testEvent = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), false);

        // have the shootingPlayer start on the pitch
        var teamLineup = new LineupDto(
                List.of(),
                List.of(),
                List.of(TestTeamPlayerDto.builder().id(shootingPlayerId).build()),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        // the main score should be 0-0
        assertEquals(0, match.getScoreInfo().getHomeGoals());
        assertEquals(0, match.getScoreInfo().getAwayGoals());
        // the half-time score should also be 0-0
        assertEquals(0, match.getHalfTimeScoreInfo().getHomeGoals());
        assertEquals(0, match.getHalfTimeScoreInfo().getAwayGoals());
        // the penalty score should remain untouched
        assertEquals(0, match.getPenaltiesInfo().getHomeGoals());
        assertEquals(0, match.getPenaltiesInfo().getAwayGoals());

        verify(matchEventRepository).save(argThat(matchEvent -> {
            MatchEventDetails.PenaltyDto pDto = (MatchEventDetails.PenaltyDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    pDto.getTeamId().equals(match.getAwayTeam().getId()) &&
                    pDto.getShootingPlayer().getTeamPlayerId().equals(shootingPlayerId) &&
                    pDto.isCountAsGoal() &&
                    !pDto.isScored();
        }));
    }

    @Test
    @DisplayName("processEvent accepts home PENALTY events happening outside the FIRST_HALF and correctly increments the scorelines when the penalty is scored")
    public void processEvent_NonFirstHalfHomePenaltyGoal_CorrectlyIncrementsScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var statuses = List.of(MatchStatus.SECOND_HALF, MatchStatus.EXTRA_TIME);

        for (MatchStatus status : statuses) {
            var match = TestMatch.builder().status(status).build();
            var matchId = match.getId();

            var shootingPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
            var shootingPlayerId = shootingPlayer.getId();

            var testEvent = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), true);

            // have the shootingPlayer start on the pitch
            var teamLineup = new LineupDto(
                    List.of(TestTeamPlayerDto.builder().id(shootingPlayerId).build()),
                    List.of(),
                    List.of(),
                    List.of()
            );

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
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
                MatchEventDetails.PenaltyDto pDto = (MatchEventDetails.PenaltyDto) matchEvent.getEvent();
                return matchEvent.getMatch().getId().equals(matchId) &&
                        pDto.getTeamId().equals(match.getHomeTeam().getId()) &&
                        pDto.getShootingPlayer().getTeamPlayerId().equals(shootingPlayerId) &&
                        pDto.isCountAsGoal() &&
                        pDto.isScored();
            }));
        }
    }

    @Test
    @DisplayName("processEvent accepts home PENALTY events happening outside the FIRST_HALF and does not increment the scorelines when the penalty is missed")
    public void processEvent_NonFirstHalfHomePenaltyMissed_DoesNotTouchScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var statuses = List.of(MatchStatus.SECOND_HALF, MatchStatus.EXTRA_TIME);

        for (MatchStatus status : statuses) {
            var match = TestMatch.builder().status(status).build();
            var matchId = match.getId();

            var shootingPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
            var shootingPlayerId = shootingPlayer.getId();

            var testEvent = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), false);

            // have the shootingPlayer start on the pitch
            var teamLineup = new LineupDto(
                    List.of(TestTeamPlayerDto.builder().id(shootingPlayerId).build()),
                    List.of(),
                    List.of(),
                    List.of()
            );

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
            given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

            // when
            matchEventService.processEvent(matchId, testEvent);

            // then
            // the main score should be 0-0
            assertEquals(0, match.getScoreInfo().getHomeGoals());
            assertEquals(0, match.getScoreInfo().getAwayGoals());
            // the half-time score should remain untouched
            assertEquals(0, match.getHalfTimeScoreInfo().getHomeGoals());
            assertEquals(0, match.getHalfTimeScoreInfo().getAwayGoals());
            // the penalty score should remain untouched
            assertEquals(0, match.getPenaltiesInfo().getHomeGoals());
            assertEquals(0, match.getPenaltiesInfo().getAwayGoals());

            verify(matchEventRepository).save(argThat(matchEvent -> {
                MatchEventDetails.PenaltyDto pDto = (MatchEventDetails.PenaltyDto) matchEvent.getEvent();
                return matchEvent.getMatch().getId().equals(matchId) &&
                        pDto.getTeamId().equals(match.getHomeTeam().getId()) &&
                        pDto.getShootingPlayer().getTeamPlayerId().equals(shootingPlayerId) &&
                        pDto.isCountAsGoal() &&
                        !pDto.isScored();
            }));
        }
    }

    @Test
    @DisplayName("processEvent accepts away PENALTY events happening outside the FIRST_HALF and correctly increments the scorelines when the penalty is scored")
    public void processEvent_NonFirstHalfAwayPenaltyGoal_CorrectlyIncrementsScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var statuses = List.of(MatchStatus.SECOND_HALF, MatchStatus.EXTRA_TIME);

        for (MatchStatus status : statuses) {
            var match = TestMatch.builder().status(status).build();
            var matchId = match.getId();

            var shootingPlayer = new TeamPlayer(match.getAwayTeam(), new Player(), Position.GOALKEEPER, 1);
            var shootingPlayerId = shootingPlayer.getId();

            var testEvent = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), true);

            // have the shootingPlayer start on the pitch
            var teamLineup = new LineupDto(
                    List.of(),
                    List.of(),
                    List.of(TestTeamPlayerDto.builder().id(shootingPlayerId).build()),
                    List.of()
            );

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
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
                MatchEventDetails.PenaltyDto pDto = (MatchEventDetails.PenaltyDto) matchEvent.getEvent();
                return matchEvent.getMatch().getId().equals(matchId) &&
                        pDto.getTeamId().equals(match.getAwayTeam().getId()) &&
                        pDto.getShootingPlayer().getTeamPlayerId().equals(shootingPlayerId) &&
                        pDto.isCountAsGoal() &&
                        pDto.isScored();
            }));
        }
    }

    @Test
    @DisplayName("processEvent accepts away PENALTY events happening outside the FIRST_HALF and does not increment the scorelines when the penalty is missed")
    public void processEvent_NonFirstHalfAwayPenaltyMissed_DoesNotTouchScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var statuses = List.of(MatchStatus.SECOND_HALF, MatchStatus.EXTRA_TIME);

        for (MatchStatus status : statuses) {
            var match = TestMatch.builder().status(status).build();
            var matchId = match.getId();

            var shootingPlayer = new TeamPlayer(match.getAwayTeam(), new Player(), Position.GOALKEEPER, 1);
            var shootingPlayerId = shootingPlayer.getId();

            var testEvent = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), false);

            // have the shootingPlayer start on the pitch
            var teamLineup = new LineupDto(
                    List.of(),
                    List.of(),
                    List.of(TestTeamPlayerDto.builder().id(shootingPlayerId).build()),
                    List.of()
            );

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
            given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

            // when
            matchEventService.processEvent(matchId, testEvent);

            // then
            // the main score should be 0-0
            assertEquals(0, match.getScoreInfo().getHomeGoals());
            assertEquals(0, match.getScoreInfo().getAwayGoals());
            // the half-time score should remain untouched
            assertEquals(0, match.getHalfTimeScoreInfo().getHomeGoals());
            assertEquals(0, match.getHalfTimeScoreInfo().getAwayGoals());
            // the penalty score should remain untouched
            assertEquals(0, match.getPenaltiesInfo().getHomeGoals());
            assertEquals(0, match.getPenaltiesInfo().getAwayGoals());

            verify(matchEventRepository).save(argThat(matchEvent -> {
                MatchEventDetails.PenaltyDto pDto = (MatchEventDetails.PenaltyDto) matchEvent.getEvent();
                return matchEvent.getMatch().getId().equals(matchId) &&
                        pDto.getTeamId().equals(match.getAwayTeam().getId()) &&
                        pDto.getShootingPlayer().getTeamPlayerId().equals(shootingPlayerId) &&
                        pDto.isCountAsGoal() &&
                        !pDto.isScored();
            }));
        }
    }

    @Test
    @DisplayName("processEvent accepts home PENALTY events happening during the PENALTIES and correctly increments the scorelines when the penalty is scored")
    public void processEvent_PenaltiesHomePenaltyGoal_CorrectlyIncrementsScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.PENALTIES).build();
        var matchId = match.getId();

        var shootingPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var shootingPlayerId = shootingPlayer.getId();

        var testEvent = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), true);

        // have the shootingPlayer start on the pitch
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(shootingPlayerId).build()),
                List.of(),
                List.of(),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        // the main score should be 0-0
        assertEquals(0, match.getScoreInfo().getHomeGoals());
        assertEquals(0, match.getScoreInfo().getAwayGoals());
        // the half-time score should remain untouched
        assertEquals(0, match.getHalfTimeScoreInfo().getHomeGoals());
        assertEquals(0, match.getHalfTimeScoreInfo().getAwayGoals());
        // the penalty score should be 1-0
        assertEquals(1, match.getPenaltiesInfo().getHomeGoals());
        assertEquals(0, match.getPenaltiesInfo().getAwayGoals());

        verify(matchEventRepository).save(argThat(matchEvent -> {
            MatchEventDetails.PenaltyDto pDto = (MatchEventDetails.PenaltyDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    pDto.getTeamId().equals(match.getHomeTeam().getId()) &&
                    pDto.getShootingPlayer().getTeamPlayerId().equals(shootingPlayerId) &&
                    !pDto.isCountAsGoal() &&
                    pDto.isScored();
        }));
    }

    @Test
    @DisplayName("processEvent accepts home PENALTY events happening during the PENALTIES and does not increment the scorelines when the penalty is missed")
    public void processEvent_PenaltiesHomePenaltyMissed_DoesNotTouchScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.PENALTIES).build();
        var matchId = match.getId();

        var shootingPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var shootingPlayerId = shootingPlayer.getId();

        var testEvent = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), false);

        // have the shootingPlayer start on the pitch
        var teamLineup = new LineupDto(
                List.of(TestTeamPlayerDto.builder().id(shootingPlayerId).build()),
                List.of(),
                List.of(),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        // the main score should be 0-0
        assertEquals(0, match.getScoreInfo().getHomeGoals());
        assertEquals(0, match.getScoreInfo().getAwayGoals());
        // the half-time score should also be 0-0
        assertEquals(0, match.getHalfTimeScoreInfo().getHomeGoals());
        assertEquals(0, match.getHalfTimeScoreInfo().getAwayGoals());
        // the penalty score should remain untouched
        assertEquals(0, match.getPenaltiesInfo().getHomeGoals());
        assertEquals(0, match.getPenaltiesInfo().getAwayGoals());

        verify(matchEventRepository).save(argThat(matchEvent -> {
            MatchEventDetails.PenaltyDto pDto = (MatchEventDetails.PenaltyDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    pDto.getTeamId().equals(match.getHomeTeam().getId()) &&
                    pDto.getShootingPlayer().getTeamPlayerId().equals(shootingPlayerId) &&
                    !pDto.isCountAsGoal() &&
                    !pDto.isScored();
        }));
    }

    @Test
    @DisplayName("processEvent accepts away PENALTY events happening during the PENALTIES and correctly increments the scorelines when the penalty is scored")
    public void processEvent_PenaltiesAwayPenaltyGoal_CorrectlyIncrementsScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.PENALTIES).build();
        var matchId = match.getId();

        var shootingPlayer = new TeamPlayer(match.getAwayTeam(), new Player(), Position.GOALKEEPER, 1);
        var shootingPlayerId = shootingPlayer.getId();

        var testEvent = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), true);

        // have the shootingPlayer start on the pitch
        var teamLineup = new LineupDto(
                List.of(),
                List.of(),
                List.of(TestTeamPlayerDto.builder().id(shootingPlayerId).build()),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        // the main score should be 0-0
        assertEquals(0, match.getScoreInfo().getHomeGoals());
        assertEquals(0, match.getScoreInfo().getAwayGoals());
        // the half-time score should also be 0-0
        assertEquals(0, match.getHalfTimeScoreInfo().getHomeGoals());
        assertEquals(0, match.getHalfTimeScoreInfo().getAwayGoals());
        // the penalty score should be 0-1
        assertEquals(0, match.getPenaltiesInfo().getHomeGoals());
        assertEquals(1, match.getPenaltiesInfo().getAwayGoals());

        verify(matchEventRepository).save(argThat(matchEvent -> {
            MatchEventDetails.PenaltyDto pDto = (MatchEventDetails.PenaltyDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    pDto.getTeamId().equals(match.getAwayTeam().getId()) &&
                    pDto.getShootingPlayer().getTeamPlayerId().equals(shootingPlayerId) &&
                    !pDto.isCountAsGoal() &&
                    pDto.isScored();
        }));
    }

    @Test
    @DisplayName("processEvent accepts away PENALTY events happening during the PENALTIES and does not increment the scorelines when the penalty is missed")
    public void processEvent_PenaltiesAwayPenaltyMissed_DoesNotTouchScorelines() throws ResourceNotFoundException, MatchEventInvalidException {
        var match = TestMatch.builder().status(MatchStatus.PENALTIES).build();
        var matchId = match.getId();

        var shootingPlayer = new TeamPlayer(match.getAwayTeam(), new Player(), Position.GOALKEEPER, 1);
        var shootingPlayerId = shootingPlayer.getId();

        var testEvent = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), false);

        // have the shootingPlayer start on the pitch
        var teamLineup = new LineupDto(
                List.of(),
                List.of(),
                List.of(TestTeamPlayerDto.builder().id(shootingPlayerId).build()),
                List.of()
        );

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        // the main score should be 0-0
        assertEquals(0, match.getScoreInfo().getHomeGoals());
        assertEquals(0, match.getScoreInfo().getAwayGoals());
        // the half-time score should also be 0-0
        assertEquals(0, match.getHalfTimeScoreInfo().getHomeGoals());
        assertEquals(0, match.getHalfTimeScoreInfo().getAwayGoals());
        // the penalty score should remain untouched
        assertEquals(0, match.getPenaltiesInfo().getHomeGoals());
        assertEquals(0, match.getPenaltiesInfo().getAwayGoals());

        verify(matchEventRepository).save(argThat(matchEvent -> {
            MatchEventDetails.PenaltyDto pDto = (MatchEventDetails.PenaltyDto) matchEvent.getEvent();
            return matchEvent.getMatch().getId().equals(matchId) &&
                    pDto.getTeamId().equals(match.getAwayTeam().getId()) &&
                    pDto.getShootingPlayer().getTeamPlayerId().equals(shootingPlayerId) &&
                    !pDto.isCountAsGoal() &&
                    !pDto.isScored();
        }));
    }
}
