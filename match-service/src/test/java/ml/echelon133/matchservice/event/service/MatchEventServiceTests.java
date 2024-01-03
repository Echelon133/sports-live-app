package ml.echelon133.matchservice.event.service;

import ml.echelon133.common.event.dto.MatchEventDetails;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.match.MatchResult;
import ml.echelon133.common.match.MatchStatus;
import ml.echelon133.matchservice.event.exceptions.MatchEventInvalidException;
import ml.echelon133.matchservice.event.model.MatchEvent;
import ml.echelon133.matchservice.event.model.dto.InsertMatchEvent;
import ml.echelon133.matchservice.event.repository.MatchEventRepository;
import ml.echelon133.matchservice.match.TestLineupDto;
import ml.echelon133.matchservice.match.TestMatch;
import ml.echelon133.matchservice.match.model.Match;
import ml.echelon133.matchservice.match.model.ScoreInfo;
import ml.echelon133.matchservice.match.service.MatchService;
import ml.echelon133.matchservice.player.model.Player;
import ml.echelon133.matchservice.player.model.Position;
import ml.echelon133.matchservice.team.TestTeam;
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

    private void givenMatchReturnEvents(UUID matchId, List<MatchEvent> matchEvents) {
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(matchEvents);
    }

    private MatchEvent createTestCardEvent(Match match, UUID receivingTeamPlayerId, MatchEventDetails.CardDto.CardType type) {
        return new MatchEvent(
            match,
            new MatchEventDetails.CardDto(
                    "1",
                    UUID.randomUUID(),
                    null,
                    type,
                    new MatchEventDetails.SerializedPlayerInfo(receivingTeamPlayerId, null, null)
            )
        );
    }

    private MatchEvent createTestSubstitutionEvent(Match match, UUID playerInId, UUID playerOutId) {
        return new MatchEvent(
            match,
            new MatchEventDetails.SubstitutionDto(
                    "1",
                    UUID.randomUUID(),
                    null,
                    new MatchEventDetails.SerializedPlayerInfo(playerInId, null, null),
                    new MatchEventDetails.SerializedPlayerInfo(playerOutId, null, null)
            )
        );
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

    private void assertMatchScoreEqual(
            Match match,
            ScoreInfo expectedHalfTimeScore,
            ScoreInfo expectedMainScore,
            ScoreInfo expectedPenaltyScore
    ) {
        assertEquals(expectedHalfTimeScore, match.getHalfTimeScoreInfo());
        assertEquals(expectedMainScore, match.getScoreInfo());
        assertEquals(expectedPenaltyScore, match.getPenaltiesInfo());
    }

    private void assertPlayerNotOnPitch(UUID matchId, InsertMatchEvent testEvent, UUID checkedPlayer) {
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        var expectedMessage = String.format("the player %s is not on the pitch", checkedPlayer);
        assertEquals(expectedMessage, message);
    }

    private void assertPlayerIsAlreadyEjected(UUID matchId, InsertMatchEvent testEvent) {
        String message = assertThrows(MatchEventInvalidException.class, () -> {
            matchEventService.processEvent(matchId, testEvent);
        }).getMessage();

        // then
        assertEquals("the player is already ejected", message);
    }

    @Test
    @DisplayName("findAllByMatchId returns an empty list when there are no events")
    public void findAllByMatchId_NoEvents_ReturnsEmptyList() {
        var matchId = UUID.randomUUID();

        // given
        givenMatchReturnEvents(matchId, List.of());

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
                match, new MatchEventDetails.StatusDto("1", match.getCompetitionId(), targetStatus, null, null)
        );

        // given
        givenMatchReturnEvents(matchId, List.of(statusEvent));

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

    @Test
    @DisplayName("processEvent of FINISHED match only processes valid STATUS changes")
    public void processEvent_MatchStatusFinished_RejectsInvalidStatusChanges() throws ResourceNotFoundException {
        var testedMatchStatus = MatchStatus.FINISHED;
        List<MatchStatus> expectedValidStatusChanges = List.of();
        assertMatchStatusCanOnlyChangeTo(testedMatchStatus, expectedValidStatusChanges);
    }

    // helper which makes sure that a match with some initial MatchStatus cannot be moved into an invalid
    // status (i.e. a match that is already FINISHED cannot be marked as NOT_STARTED, etc.)
    private void assertMatchStatusCanOnlyChangeTo(
            MatchStatus testedMatchStatus,
            List<MatchStatus> expectedValidStatusChanges
    ) throws ResourceNotFoundException {

        var match = TestMatch.builder()
                // set both scorelines to an impossible state to avoid throwing on checks that do not allow
                // the extra-time or penalties to end in a draw
                .scoreInfo(ScoreInfo.of(2, 1))
                .penaltiesInfo(ScoreInfo.of(3, 1))
                .build();
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
    @DisplayName("processEvent correctly sets the final result of the match when the home side wins in regular time")
    public void processEvent_HomeSideWinsAfterRegularTime_SetsCorrectFinalResult()
            throws ResourceNotFoundException, MatchEventInvalidException {

        var testedScorelines = List.of(
                ScoreInfo.of(1, 0), ScoreInfo.of(2, 1), ScoreInfo.of(3, 2), ScoreInfo.of(5, 1)
        );

        for (ScoreInfo testedScoreline: testedScorelines) {
            // reset the match after every tested scoreline
            var match = TestMatch.builder()
                    .status(MatchStatus.SECOND_HALF)
                    .scoreInfo(testedScoreline)
                    .build();
            var matchId = match.getId();

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            var testedEvent = new InsertMatchEvent.StatusDto("90", MatchStatus.FINISHED.name());

            // when
            matchEventService.processEvent(matchId, testedEvent);

            // then
            var expectedResult = MatchResult.HOME_WIN;
            assertEquals(expectedResult, match.getResult());
            verify(matchEventRepository).save(argThat(e -> {
                MatchEventDetails.StatusDto sDto = (MatchEventDetails.StatusDto) e.getEvent();
                var teams = sDto.getTeams();
                return e.getMatch().getId().equals(matchId) &&
                        sDto.getResult().equals(expectedResult) &&
                        teams.getHomeTeamId().equals(match.getHomeTeam().getId()) &&
                        teams.getAwayTeamId().equals(match.getAwayTeam().getId());
            }));
        }
    }

    @Test
    @DisplayName("processEvent correctly sets the final result of the match when the away side wins in regular time")
    public void processEvent_AwaySideWinsAfterRegularTime_SetsCorrectFinalResult()
            throws ResourceNotFoundException, MatchEventInvalidException {

        var testedScorelines = List.of(
                ScoreInfo.of(0, 1), ScoreInfo.of(1, 2), ScoreInfo.of(2, 3), ScoreInfo.of(1, 5)
        );

        for (ScoreInfo testedScoreline: testedScorelines) {
            // reset the match after every tested scoreline
            var match = TestMatch.builder()
                    .status(MatchStatus.SECOND_HALF)
                    .scoreInfo(testedScoreline)
                    .build();
            var matchId = match.getId();

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            var testedEvent = new InsertMatchEvent.StatusDto("90", MatchStatus.FINISHED.name());

            // when
            matchEventService.processEvent(matchId, testedEvent);

            // then
            var expectedResult = MatchResult.AWAY_WIN;
            assertEquals(expectedResult, match.getResult());
            verify(matchEventRepository).save(argThat(e -> {
                MatchEventDetails.StatusDto sDto = (MatchEventDetails.StatusDto) e.getEvent();
                var teams = sDto.getTeams();
                return e.getMatch().getId().equals(matchId) &&
                        sDto.getResult().equals(expectedResult) &&
                        teams.getHomeTeamId().equals(match.getHomeTeam().getId()) &&
                        teams.getAwayTeamId().equals(match.getAwayTeam().getId());
            }));
        }
    }

    @Test
    @DisplayName("processEvent correctly sets the final result of the match when there is a draw in regular time")
    public void processEvent_DrawAfterRegularTime_SetsCorrectFinalResult()
            throws ResourceNotFoundException, MatchEventInvalidException {

        var testedScorelines = List.of(
                ScoreInfo.of(1, 1), ScoreInfo.of(2, 2), ScoreInfo.of(3, 3), ScoreInfo.of(5, 5)
        );

        for (ScoreInfo testedScoreline: testedScorelines) {
            // reset the match after every tested scoreline
            var match = TestMatch.builder()
                    .status(MatchStatus.SECOND_HALF)
                    .scoreInfo(testedScoreline)
                    .build();
            var matchId = match.getId();

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            var testedEvent = new InsertMatchEvent.StatusDto("90", MatchStatus.FINISHED.name());

            // when
            matchEventService.processEvent(matchId, testedEvent);

            // then
            var expectedResult = MatchResult.DRAW;
            assertEquals(expectedResult, match.getResult());
            verify(matchEventRepository).save(argThat(e -> {
                MatchEventDetails.StatusDto sDto = (MatchEventDetails.StatusDto) e.getEvent();
                var teams = sDto.getTeams();
                return e.getMatch().getId().equals(matchId) &&
                        sDto.getResult().equals(expectedResult) &&
                        teams.getHomeTeamId().equals(match.getHomeTeam().getId()) &&
                        teams.getAwayTeamId().equals(match.getAwayTeam().getId());
            }));
        }
    }

    @Test
    @DisplayName("processEvent correctly sets the final result of the match when the home side wins in extra time")
    public void processEvent_HomeSideWinsAfterExtraTime_SetsCorrectFinalResult()
            throws ResourceNotFoundException, MatchEventInvalidException {

        var testedScorelines = List.of(
                ScoreInfo.of(1, 0), ScoreInfo.of(2, 1), ScoreInfo.of(3, 2), ScoreInfo.of(5, 1)
        );

        for (ScoreInfo testedScoreline: testedScorelines) {
            // reset the match after every tested scoreline
            var match = TestMatch.builder()
                    .status(MatchStatus.EXTRA_TIME)
                    .scoreInfo(testedScoreline)
                    .build();
            var matchId = match.getId();

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            var testedEvent = new InsertMatchEvent.StatusDto("120", MatchStatus.FINISHED.name());

            // when
            matchEventService.processEvent(matchId, testedEvent);

            // then
            var expectedResult = MatchResult.HOME_WIN;
            assertEquals(expectedResult, match.getResult());
            verify(matchEventRepository).save(argThat(e -> {
                MatchEventDetails.StatusDto sDto = (MatchEventDetails.StatusDto) e.getEvent();
                var teams = sDto.getTeams();
                return e.getMatch().getId().equals(matchId) &&
                        sDto.getResult().equals(expectedResult) &&
                        teams.getHomeTeamId().equals(match.getHomeTeam().getId()) &&
                        teams.getAwayTeamId().equals(match.getAwayTeam().getId());
            }));
        }
    }

    @Test
    @DisplayName("processEvent correctly sets the final result of the match when the away side wins in extra time")
    public void processEvent_AwaySideWinsAfterExtraTime_SetsCorrectFinalResult()
            throws ResourceNotFoundException, MatchEventInvalidException {

        var testedScorelines = List.of(
                ScoreInfo.of(0, 1), ScoreInfo.of(1, 2), ScoreInfo.of(2, 3), ScoreInfo.of(1, 5)
        );

        for (ScoreInfo testedScoreline: testedScorelines) {
            // reset the match after every tested scoreline
            var match = TestMatch.builder()
                    .status(MatchStatus.EXTRA_TIME)
                    .scoreInfo(testedScoreline)
                    .build();
            var matchId = match.getId();

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            var testedEvent = new InsertMatchEvent.StatusDto("120", MatchStatus.FINISHED.name());

            // when
            matchEventService.processEvent(matchId, testedEvent);

            // then
            var expectedResult = MatchResult.AWAY_WIN;
            assertEquals(expectedResult, match.getResult());
            verify(matchEventRepository).save(argThat(e -> {
                MatchEventDetails.StatusDto sDto = (MatchEventDetails.StatusDto) e.getEvent();
                var teams = sDto.getTeams();
                return e.getMatch().getId().equals(matchId) &&
                        sDto.getResult().equals(expectedResult) &&
                        teams.getHomeTeamId().equals(match.getHomeTeam().getId()) &&
                        teams.getAwayTeamId().equals(match.getAwayTeam().getId());
            }));
        }
    }

    @Test
    @DisplayName("processEvent rejects the STATUS event if the final result of the match is a draw in the extra time")
    public void processEvent_DrawAfterExtraTime_RejectsInvalidStatus() throws ResourceNotFoundException {

        var testedScorelines = List.of(
                ScoreInfo.of(1, 1), ScoreInfo.of(2, 2), ScoreInfo.of(3, 3), ScoreInfo.of(5, 5)
        );

        for (ScoreInfo testedScoreline: testedScorelines) {
            // reset the match after every tested scoreline
            var match = TestMatch.builder()
                    .status(MatchStatus.EXTRA_TIME)
                    .scoreInfo(testedScoreline)
                    .build();
            var matchId = match.getId();

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            var testedEvent = new InsertMatchEvent.StatusDto("120", MatchStatus.FINISHED.name());

            // when
            String message = assertThrows(MatchEventInvalidException.class, () -> {
                matchEventService.processEvent(matchId, testedEvent);
            }).getMessage();

            // then
            assertEquals("match cannot finish after extra time when the score is a draw", message);
        }
    }

    @Test
    @DisplayName("processEvent correctly sets the final result of the match when the home side wins in penalties")
    public void processEvent_HomeSideWinsAfterPenalties_SetsCorrectFinalResult()
            throws ResourceNotFoundException, MatchEventInvalidException {

        var testedScorelines = List.of(
                ScoreInfo.of(5, 3), ScoreInfo.of(7, 5), ScoreInfo.of(6, 4), ScoreInfo.of(8, 6)
        );

        for (ScoreInfo testedScoreline: testedScorelines) {
            // reset the match after every tested scoreline
            var match = TestMatch.builder()
                    .status(MatchStatus.PENALTIES)
                    .penaltiesInfo(testedScoreline)
                    .build();
            var matchId = match.getId();

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            var testedEvent = new InsertMatchEvent.StatusDto("120", MatchStatus.FINISHED.name());

            // when
            matchEventService.processEvent(matchId, testedEvent);

            // then
            var expectedResult = MatchResult.HOME_WIN;
            assertEquals(expectedResult, match.getResult());
            verify(matchEventRepository).save(argThat(e -> {
                MatchEventDetails.StatusDto sDto = (MatchEventDetails.StatusDto) e.getEvent();
                var teams = sDto.getTeams();
                return e.getMatch().getId().equals(matchId) &&
                        sDto.getResult().equals(expectedResult) &&
                        teams.getHomeTeamId().equals(match.getHomeTeam().getId()) &&
                        teams.getAwayTeamId().equals(match.getAwayTeam().getId());
            }));
        }
    }

    @Test
    @DisplayName("processEvent correctly sets the final result of the match when the away side wins in penalties")
    public void processEvent_AwaySideWinsAfterPenalties_SetsCorrectFinalResult()
            throws ResourceNotFoundException, MatchEventInvalidException {

        var testedScorelines = List.of(
                ScoreInfo.of(3, 5), ScoreInfo.of(5, 7), ScoreInfo.of(4, 6), ScoreInfo.of(6, 8)
        );

        for (ScoreInfo testedScoreline: testedScorelines) {
            // reset the match after every tested scoreline
            var match = TestMatch.builder()
                    .status(MatchStatus.PENALTIES)
                    .penaltiesInfo(testedScoreline)
                    .build();
            var matchId = match.getId();

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            var testedEvent = new InsertMatchEvent.StatusDto("120", MatchStatus.FINISHED.name());

            // when
            matchEventService.processEvent(matchId, testedEvent);

            // then
            var expectedResult = MatchResult.AWAY_WIN;
            assertEquals(expectedResult, match.getResult());
            verify(matchEventRepository).save(argThat(e -> {
                MatchEventDetails.StatusDto sDto = (MatchEventDetails.StatusDto) e.getEvent();
                var teams = sDto.getTeams();
                return e.getMatch().getId().equals(matchId) &&
                        sDto.getResult().equals(expectedResult) &&
                        teams.getHomeTeamId().equals(match.getHomeTeam().getId()) &&
                        teams.getAwayTeamId().equals(match.getAwayTeam().getId());
            }));
        }
    }

    @Test
    @DisplayName("processEvent rejects the STATUS event if the final result of the match is a draw after the penalties")
    public void processEvent_DrawAfterPenalties_RejectsInvalidStatus() throws ResourceNotFoundException {

        var testedScorelines = List.of(
                ScoreInfo.of(5, 5), ScoreInfo.of(6, 6), ScoreInfo.of(8, 8), ScoreInfo.of(12, 12)
        );

        for (ScoreInfo testedScoreline: testedScorelines) {
            // reset the match after every tested scoreline
            var match = TestMatch.builder()
                    .status(MatchStatus.PENALTIES)
                    .penaltiesInfo(testedScoreline)
                    .build();
            var matchId = match.getId();

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            var testedEvent = new InsertMatchEvent.StatusDto("120", MatchStatus.FINISHED.name());

            // when
            String message = assertThrows(MatchEventInvalidException.class, () -> {
                matchEventService.processEvent(matchId, testedEvent);
            }).getMessage();

            // then
            assertEquals("match cannot finish after penalties when the score is a draw", message);
        }
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
        var teamLineup = TestLineupDto.builder().build();

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
        var teamLineup = TestLineupDto.builder().homeStarting(testTeamPlayerId).build();

        // given
        givenMatchReturnEvents(matchId, List.of());
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(testTeamPlayerId)).willReturn(testTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

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
        var teamLineup = TestLineupDto.builder().homeStarting(testTeamPlayerId).build();

        // given
        givenMatchReturnEvents(matchId, List.of());
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(testTeamPlayerId)).willReturn(testTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

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
        var teamLineup = TestLineupDto.builder().homeStarting(testTeamPlayerId).build();

        // given
        givenMatchReturnEvents(matchId, List.of(
                createTestCardEvent(match, testTeamPlayerId, MatchEventDetails.CardDto.CardType.YELLOW)
        ));
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(testTeamPlayerId)).willReturn(testTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

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
        var teamLineup = TestLineupDto.builder().homeStarting(testTeamPlayerId).build();

        // given
        givenMatchReturnEvents(matchId, List.of(
                createTestCardEvent(match, testTeamPlayerId, MatchEventDetails.CardDto.CardType.YELLOW)
        ));
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(testTeamPlayerId)).willReturn(testTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

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
        var teamLineup = TestLineupDto.builder().homeStarting(testTeamPlayerId).build();

        // given
        givenMatchReturnEvents(matchId, List.of(
                createTestCardEvent(match, testTeamPlayerId, MatchEventDetails.CardDto.CardType.SECOND_YELLOW)
        ));
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(testTeamPlayerId)).willReturn(testTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // then
        assertPlayerIsAlreadyEjected(matchId, testEvent);
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
        var teamLineup = TestLineupDto.builder().homeStarting(testTeamPlayerId).build();

        // given
        givenMatchReturnEvents(matchId, List.of(
                createTestCardEvent(match, testTeamPlayerId, MatchEventDetails.CardDto.CardType.SECOND_YELLOW)
        ));
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(testTeamPlayerId)).willReturn(testTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // then
        assertPlayerIsAlreadyEjected(matchId, testEvent);
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
        var teamLineup = TestLineupDto.builder().homeStarting(testTeamPlayerId).build();

        // given
        givenMatchReturnEvents(matchId, List.of(
                createTestCardEvent(match, testTeamPlayerId, MatchEventDetails.CardDto.CardType.DIRECT_RED)
        ));
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(testTeamPlayerId)).willReturn(testTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // then
        assertPlayerIsAlreadyEjected(matchId, testEvent);
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
        var teamLineup = TestLineupDto.builder().homeStarting(testTeamPlayerId).build();

        // given
        givenMatchReturnEvents(matchId, List.of(
            createTestCardEvent(match, testTeamPlayerId, MatchEventDetails.CardDto.CardType.DIRECT_RED)
        ));
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(testTeamPlayerId)).willReturn(testTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // then
        assertPlayerIsAlreadyEjected(matchId, testEvent);
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

        var teamLineup = TestLineupDto.builder().homeStarting(scoringTeamPlayerId).build();

        // given
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(scoringTeamPlayerId)).willReturn(scoringTeamPlayer);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, scoringTeamPlayerId);
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
        var teamLineup = TestLineupDto.builder().build();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(scoringTeamPlayerId)).willReturn(scoringTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, scoringTeamPlayerId);
    }

    @Test
    @DisplayName("processEvent rejects GOAL events if the scoring player got two yellow cards")
    public void processEvent_ScoringPlayerHasTwoYellowCards_RejectsInvalidGoal() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var scoringPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var scoringPlayerId = scoringPlayer.getId();

        var testEvent = new InsertMatchEvent.GoalDto("1", scoringPlayerId.toString(), null, false);

        // given
        givenMatchReturnEvents(matchId, List.of(
                createTestCardEvent(match, scoringPlayerId, MatchEventDetails.CardDto.CardType.SECOND_YELLOW)
        ));
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(scoringPlayerId)).willReturn(scoringPlayer);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, scoringPlayerId);
    }

    @Test
    @DisplayName("processEvent rejects GOAL events if the scoring player got a red card")
    public void processEvent_ScoringPlayerHasRedCard_RejectsInvalidGoal() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var scoringPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var scoringPlayerId = scoringPlayer.getId();

        var testEvent = new InsertMatchEvent.GoalDto("1", scoringPlayerId.toString(), null, false);

        // given
        givenMatchReturnEvents(matchId, List.of(
                createTestCardEvent(match, scoringPlayerId, MatchEventDetails.CardDto.CardType.DIRECT_RED)
        ));
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(scoringPlayerId)).willReturn(scoringPlayer);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, scoringPlayerId);
    }

    @Test
    @DisplayName("processEvent rejects GOAL events if the scoring player got already subbed off")
    public void processEvent_ScoringPlayerAlreadySubbedOff_RejectsInvalidGoal() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var scoringPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var scoringPlayerId = scoringPlayer.getId();

        var testEvent = new InsertMatchEvent.GoalDto("1", scoringPlayerId.toString(), null, false);

        // given
        givenMatchReturnEvents(matchId, List.of(
                createTestSubstitutionEvent(match, UUID.randomUUID(), scoringPlayerId)
        ));
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(scoringPlayerId)).willReturn(scoringPlayer);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, scoringPlayerId);
    }

    @Test
    @DisplayName("processEvent rejects GOAL events if the scoring player spent the entire match on the bench")
    public void processEvent_ScoringPlayerConstantlyOnBench_RejectsInvalidGoal() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var scoringPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var scoringPlayerId = scoringPlayer.getId();

        var testEvent = new InsertMatchEvent.GoalDto("1", scoringPlayerId.toString(), null, false);

        // put the scoring player in the starting home lineup
        var teamLineup = TestLineupDto.builder().homeSubstitutes(scoringPlayerId).build();

        // given
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(scoringPlayerId)).willReturn(scoringPlayer);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, scoringPlayerId);
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
        var teamLineup = TestLineupDto.builder().homeStarting(scoringTeamPlayerId).build();

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
        var teamLineup = TestLineupDto.builder().homeStarting(scoringTeamPlayerId).build();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                scoringTeamPlayerId, Optional.of(scoringTeamPlayer),
                assistingTeamPlayerId, Optional.of(assistingTeamPlayer)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, assistingTeamPlayerId);
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
        var teamLineup = TestLineupDto.builder().homeStarting(scoringTeamPlayerId).build();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                scoringTeamPlayerId, Optional.of(scoringTeamPlayer),
                assistingTeamPlayerId, Optional.of(assistingTeamPlayer)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, assistingTeamPlayerId);
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
        var teamLineup = TestLineupDto.builder()
                .homeStarting(scoringTeamPlayerId)
                .awayStarting(assistingTeamPlayerId)
                .build();

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
    @DisplayName("processEvent rejects GOAL events if the assisting player got two yellow cards")
    public void processEvent_AssistingPlayerHasTwoYellowCards_RejectsInvalidGoal() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var scoringPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var scoringPlayerId = scoringPlayer.getId();
        // this player also plays in the match
        var assistingPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var assistingPlayerId = assistingPlayer.getId();

        var testEvent = new InsertMatchEvent.GoalDto("1", scoringPlayerId.toString(), assistingPlayerId.toString(), false);

        // put the scoring and assisting players in the starting home lineup
        var teamLineup = TestLineupDto.builder().homeStarting(scoringPlayerId, assistingPlayerId).build();

        // given
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        givenMatchReturnEvents(matchId, List.of(
                createTestCardEvent(match, assistingPlayerId, MatchEventDetails.CardDto.CardType.SECOND_YELLOW)
        ));
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                scoringPlayerId, Optional.of(scoringPlayer),
                assistingPlayerId, Optional.of(assistingPlayer)
        ));
        given(teamPlayerService.findEntityById(scoringPlayerId)).willReturn(scoringPlayer);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, assistingPlayerId);
    }

    @Test
    @DisplayName("processEvent rejects GOAL events if the assisting player got a red card")
    public void processEvent_AssistingPlayerHasRedCard_RejectsInvalidGoal() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var scoringPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var scoringPlayerId = scoringPlayer.getId();
        // this player also plays in the match
        var assistingPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var assistingPlayerId = assistingPlayer.getId();

        var testEvent = new InsertMatchEvent.GoalDto("1", scoringPlayerId.toString(), assistingPlayerId.toString(), false);

        // put the scoring and assisting players in the starting home lineup
        var teamLineup = TestLineupDto.builder().homeStarting(scoringPlayerId, assistingPlayerId).build();

        // given
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        givenMatchReturnEvents(matchId, List.of(
                createTestCardEvent(match, assistingPlayerId, MatchEventDetails.CardDto.CardType.DIRECT_RED)
        ));
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                scoringPlayerId, Optional.of(scoringPlayer),
                assistingPlayerId, Optional.of(assistingPlayer)
        ));
        given(teamPlayerService.findEntityById(scoringPlayerId)).willReturn(scoringPlayer);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, assistingPlayerId);
    }

    @Test
    @DisplayName("processEvent rejects GOAL events if the assisting player got already subbed off")
    public void processEvent_AssistingPlayerAlreadySubbedOff_RejectsInvalidGoal() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var scoringPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var scoringPlayerId = scoringPlayer.getId();
        // this player also plays in the match
        var assistingPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var assistingPlayerId = assistingPlayer.getId();

        var testEvent = new InsertMatchEvent.GoalDto("1", scoringPlayerId.toString(), assistingPlayerId.toString(), false);

        // put the scoring and assisting players in the starting home lineup
        var teamLineup = TestLineupDto.builder().homeStarting(scoringPlayerId, assistingPlayerId).build();

        // given
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        givenMatchReturnEvents(matchId, List.of(
                createTestSubstitutionEvent(match, UUID.randomUUID(), assistingPlayerId)
        ));
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                scoringPlayerId, Optional.of(scoringPlayer),
                assistingPlayerId, Optional.of(assistingPlayer)
        ));
        given(teamPlayerService.findEntityById(scoringPlayerId)).willReturn(scoringPlayer);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, assistingPlayerId);
    }

    @Test
    @DisplayName("processEvent rejects GOAL events if the assisting player spent the entire match on the bench")
    public void processEvent_AssistingPlayerConstantlyOnBench_RejectsInvalidGoal() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        // this player plays in the match
        var scoringPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var scoringPlayerId = scoringPlayer.getId();
        // this player also plays in the match
        var assistingPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var assistingPlayerId = assistingPlayer.getId();

        var testEvent = new InsertMatchEvent.GoalDto("1", scoringPlayerId.toString(), assistingPlayerId.toString(), false);

        // put the assisting player on the bench
        var teamLineup = TestLineupDto.builder()
                .homeStarting(scoringPlayerId)
                .homeSubstitutes(assistingPlayerId)
                .build();

        // given
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                scoringPlayerId, Optional.of(scoringPlayer),
                assistingPlayerId, Optional.of(assistingPlayer)
        ));
        given(teamPlayerService.findEntityById(scoringPlayerId)).willReturn(scoringPlayer);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, assistingPlayerId);
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
        var teamLineup = TestLineupDto.builder()
                .homeStarting(scoringTeamPlayerId, assistingTeamPlayerId)
                .build();

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
        var expectedHalfTimeScore = ScoreInfo.of(1, 0);
        var expectedMainScore = ScoreInfo.of(1, 0);
        var expectedPenaltyScore = ScoreInfo.of(0, 0);
        assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
        var teamLineup = TestLineupDto.builder().homeStarting(scoringTeamPlayerId).build();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(scoringTeamPlayerId)).willReturn(scoringTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        var expectedHalfTimeScore = ScoreInfo.of(0, 1);
        var expectedMainScore = ScoreInfo.of(0, 1);
        var expectedPenaltyScore = ScoreInfo.of(0, 0);
        assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
        var teamLineup = TestLineupDto.builder().awayStarting(scoringTeamPlayerId, assistingTeamPlayerId)
                .build();

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
        var expectedHalfTimeScore = ScoreInfo.of(0, 1);
        var expectedMainScore = ScoreInfo.of(0, 1);
        var expectedPenaltyScore = ScoreInfo.of(0, 0);
        assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
        var teamLineup = TestLineupDto.builder().awayStarting(scoringTeamPlayerId).build();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(scoringTeamPlayerId)).willReturn(scoringTeamPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        var expectedHalfTimeScore = ScoreInfo.of(1, 0);
        var expectedMainScore = ScoreInfo.of(1, 0);
        var expectedPenaltyScore = ScoreInfo.of(0, 0);
        assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
            var teamLineup = TestLineupDto.builder()
                    .homeStarting(scoringTeamPlayerId, assistingTeamPlayerId)
                    .build();

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
            var expectedHalfTimeScore = ScoreInfo.of(0, 0);
            var expectedMainScore = ScoreInfo.of(1, 0);
            var expectedPenaltyScore = ScoreInfo.of(0, 0);
            assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
            var teamLineup = TestLineupDto.builder().homeStarting(scoringTeamPlayerId).build();

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            given(teamPlayerService.findEntityById(scoringTeamPlayerId)).willReturn(scoringTeamPlayer);
            given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

            // when
            matchEventService.processEvent(matchId, testEvent);

            // then
            var expectedHalfTimeScore = ScoreInfo.of(0, 0);
            var expectedMainScore = ScoreInfo.of(0, 1);
            var expectedPenaltyScore = ScoreInfo.of(0, 0);
            assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
            var teamLineup = TestLineupDto.builder()
                    .awayStarting(scoringTeamPlayerId, assistingTeamPlayerId)
                    .build();

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
            var expectedHalfTimeScore = ScoreInfo.of(0, 0);
            var expectedMainScore = ScoreInfo.of(0, 1);
            var expectedPenaltyScore = ScoreInfo.of(0, 0);
            assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
            var teamLineup = TestLineupDto.builder().awayStarting(scoringTeamPlayerId).build();

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            given(teamPlayerService.findEntityById(scoringTeamPlayerId)).willReturn(scoringTeamPlayer);
            given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

            // when
            matchEventService.processEvent(matchId, testEvent);

            // then
            var expectedHalfTimeScore = ScoreInfo.of(0, 0);
            var expectedMainScore = ScoreInfo.of(1, 0);
            var expectedPenaltyScore = ScoreInfo.of(0, 0);
            assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
        var teamLineup = TestLineupDto.builder().build();

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
        var teamLineup = TestLineupDto.builder().homeStarting(teamPlayerInId).build();

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
        var teamLineup = TestLineupDto.builder().homeStarting(teamPlayerInId).build();

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
        var teamLineup = TestLineupDto.builder()
                .homeStarting(teamPlayerInId)
                .awayStarting(teamPlayerOutId)
                .build();

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
        var teamLineup = TestLineupDto.builder()
                .homeStarting(teamPlayerInId, teamPlayerOutId)
                .build();

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
        var teamLineup = TestLineupDto.builder()
                .homeStarting(teamPlayerOutId)
                .homeSubstitutes(teamPlayerInId)
                .build();

        // given
        givenMatchReturnEvents(matchId, List.of(
               createTestCardEvent(match, teamPlayerInId, MatchEventDetails.CardDto.CardType.SECOND_YELLOW)
        ));
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
        var teamLineup = TestLineupDto.builder()
                .homeStarting(teamPlayerOutId)
                .homeSubstitutes(teamPlayerInId)
                .build();

        // given
        givenMatchReturnEvents(matchId, List.of(
                createTestCardEvent(match, teamPlayerInId, MatchEventDetails.CardDto.CardType.DIRECT_RED)
        ));
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
        var teamLineup = TestLineupDto.builder()
                .homeStarting(teamPlayerOutId)
                .homeSubstitutes(teamPlayerInId)
                .build();

        // given
        givenMatchReturnEvents(matchId, List.of(
                createTestSubstitutionEvent(match, teamPlayerInId, teamPlayerOutId)
        ));
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
        var teamLineup = TestLineupDto.builder()
                .homeStarting(teamPlayerOutId)
                .homeSubstitutes(teamPlayerInId)
                .build();

        // given
        givenMatchReturnEvents(matchId, List.of(
                createTestCardEvent(match, teamPlayerOutId, MatchEventDetails.CardDto.CardType.SECOND_YELLOW)
        ));
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                teamPlayerInId, Optional.of(teamPlayerIn),
                teamPlayerOutId, Optional.of(teamPlayerOut)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, teamPlayerOutId);
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
        var teamLineup = TestLineupDto.builder()
                .homeStarting(teamPlayerOutId)
                .homeSubstitutes(teamPlayerInId)
                .build();

        // given
        givenMatchReturnEvents(matchId, List.of(
                createTestCardEvent(match, teamPlayerOutId, MatchEventDetails.CardDto.CardType.DIRECT_RED)
        ));
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                teamPlayerInId, Optional.of(teamPlayerIn),
                teamPlayerOutId, Optional.of(teamPlayerOut)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, teamPlayerOutId);
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
        var teamLineup = TestLineupDto.builder()
                .homeStarting(teamPlayerOutId)
                .homeSubstitutes(teamPlayerInId)
                .build();

        // given
        givenMatchReturnEvents(matchId, List.of(
                createTestSubstitutionEvent(match, UUID.randomUUID(), teamPlayerOutId)
        ));
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                teamPlayerInId, Optional.of(teamPlayerIn),
                teamPlayerOutId, Optional.of(teamPlayerOut)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, teamPlayerOutId);
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
        var teamLineup = TestLineupDto.builder()
                .homeSubstitutes(teamPlayerOutId, teamPlayerInId)
                .build();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        bindTeamPlayerIdsToTeamPlayers(Map.of(
                teamPlayerInId, Optional.of(teamPlayerIn),
                teamPlayerOutId, Optional.of(teamPlayerOut)
        ));
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, teamPlayerOutId);
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
        var teamLineup = TestLineupDto.builder()
                .homeStarting(teamPlayerOutId)
                .homeSubstitutes(teamPlayerInId)
                .build();

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
        var teamLineup = TestLineupDto.builder()
                .homeStarting(teamPlayerOutId)
                .homeSubstitutes(teamPlayerInId)
                .build();

        // given
        givenMatchReturnEvents(matchId, List.of(
                // give both players yellow cards
                createTestCardEvent(match, teamPlayerInId, MatchEventDetails.CardDto.CardType.YELLOW),
                createTestCardEvent(match, teamPlayerOutId, MatchEventDetails.CardDto.CardType.YELLOW)
        ));
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
        var teamLineup = TestLineupDto.builder()
                .homeSubstitutes(teamPlayerOutId, teamPlayerInId)
                .build();

        // given
        givenMatchReturnEvents(matchId, List.of(
                createTestSubstitutionEvent(match, teamPlayerOutId, UUID.randomUUID())
        ));
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

        // given
        givenMatchReturnEvents(matchId, List.of(
                createTestCardEvent(match, shootingPlayerId, MatchEventDetails.CardDto.CardType.SECOND_YELLOW)
        ));
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, shootingPlayerId);
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

        // given
        givenMatchReturnEvents(matchId, List.of(
                createTestCardEvent(match, shootingPlayerId, MatchEventDetails.CardDto.CardType.DIRECT_RED)
        ));
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, shootingPlayerId);
    }

    @Test
    @DisplayName("processEvent rejects PENALTY events if the shooting player got already subbed off")
    public void processEvent_PlayerOutAlreadySubbedOff_RejectsInvalidPenalty() throws ResourceNotFoundException {
        var match = TestMatch.builder().status(MatchStatus.FIRST_HALF).build();
        var matchId = match.getId();

        var shootingPlayer = new TeamPlayer(match.getHomeTeam(), new Player(), Position.GOALKEEPER, 1);
        var shootingPlayerId = shootingPlayer.getId();

        var testEvent = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), true);

        // given
        givenMatchReturnEvents(matchId, List.of(
                createTestSubstitutionEvent(match, UUID.randomUUID(), shootingPlayerId)
        ));
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, shootingPlayerId);
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
        var teamLineup = TestLineupDto.builder()
                .homeSubstitutes(shootingPlayerId)
                .build();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // then
        assertPlayerNotOnPitch(matchId, testEvent, shootingPlayerId);
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
        var teamLineup = TestLineupDto.builder()
                .homeStarting(shootingPlayerId)
                .build();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        var expectedHalfTimeScore = ScoreInfo.of(1, 0);
        var expectedMainScore = ScoreInfo.of(1, 0);
        var expectedPenaltyScore = ScoreInfo.of(0, 0);
        assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
        var teamLineup = TestLineupDto.builder()
                .homeStarting(shootingPlayerId)
                .build();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        var expectedHalfTimeScore = ScoreInfo.of(0, 0);
        var expectedMainScore = ScoreInfo.of(0, 0);
        var expectedPenaltyScore = ScoreInfo.of(0, 0);
        assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
        var teamLineup = TestLineupDto.builder()
                .awayStarting(shootingPlayerId)
                .build();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        var expectedHalfTimeScore = ScoreInfo.of(0, 1);
        var expectedMainScore = ScoreInfo.of(0, 1);
        var expectedPenaltyScore = ScoreInfo.of(0, 0);
        assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
        var teamLineup = TestLineupDto.builder()
                .awayStarting(shootingPlayerId)
                .build();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        var expectedHalfTimeScore = ScoreInfo.of(0, 0);
        var expectedMainScore = ScoreInfo.of(0, 0);
        var expectedPenaltyScore = ScoreInfo.of(0, 0);
        assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
            var teamLineup = TestLineupDto.builder()
                    .homeStarting(shootingPlayerId)
                    .build();

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
            given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

            // when
            matchEventService.processEvent(matchId, testEvent);

            // then
            var expectedHalfTimeScore = ScoreInfo.of(0, 0);
            var expectedMainScore = ScoreInfo.of(1, 0);
            var expectedPenaltyScore = ScoreInfo.of(0, 0);
            assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
            var teamLineup = TestLineupDto.builder()
                    .homeStarting(shootingPlayerId)
                    .build();

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
            given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

            // when
            matchEventService.processEvent(matchId, testEvent);

            // then
            var expectedHalfTimeScore = ScoreInfo.of(0, 0);
            var expectedMainScore = ScoreInfo.of(0, 0);
            var expectedPenaltyScore = ScoreInfo.of(0, 0);
            assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
            var teamLineup = TestLineupDto.builder()
                    .awayStarting(shootingPlayerId)
                    .build();

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
            given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

            // when
            matchEventService.processEvent(matchId, testEvent);

            // then
            var expectedHalfTimeScore = ScoreInfo.of(0, 0);
            var expectedMainScore = ScoreInfo.of(0, 1);
            var expectedPenaltyScore = ScoreInfo.of(0, 0);
            assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
            var teamLineup = TestLineupDto.builder()
                    .awayStarting(shootingPlayerId)
                    .build();

            // given
            given(matchService.findEntityById(matchId)).willReturn(match);
            given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
            given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

            // when
            matchEventService.processEvent(matchId, testEvent);

            // then
            var expectedHalfTimeScore = ScoreInfo.of(0, 0);
            var expectedMainScore = ScoreInfo.of(0, 0);
            var expectedPenaltyScore = ScoreInfo.of(0, 0);
            assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
        var teamLineup = TestLineupDto.builder()
                .homeStarting(shootingPlayerId)
                .build();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        var expectedHalfTimeScore = ScoreInfo.of(0, 0);
        var expectedMainScore = ScoreInfo.of(0, 0);
        var expectedPenaltyScore = ScoreInfo.of(1, 0);
        assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
        var teamLineup = TestLineupDto.builder()
                .homeStarting(shootingPlayerId)
                .build();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        var expectedHalfTimeScore = ScoreInfo.of(0, 0);
        var expectedMainScore = ScoreInfo.of(0, 0);
        var expectedPenaltyScore = ScoreInfo.of(0, 0);
        assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
        var teamLineup = TestLineupDto.builder()
                .awayStarting(shootingPlayerId)
                .build();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        var expectedHalfTimeScore = ScoreInfo.of(0, 0);
        var expectedMainScore = ScoreInfo.of(0, 0);
        var expectedPenaltyScore = ScoreInfo.of(0, 1);
        assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
        var teamLineup = TestLineupDto.builder()
                .awayStarting(shootingPlayerId)
                .build();

        // given
        given(matchService.findEntityById(matchId)).willReturn(match);
        given(teamPlayerService.findEntityById(shootingPlayerId)).willReturn(shootingPlayer);
        given(matchService.findMatchLineup(matchId)).willReturn(teamLineup);

        // when
        matchEventService.processEvent(matchId, testEvent);

        // then
        var expectedHalfTimeScore = ScoreInfo.of(0, 0);
        var expectedMainScore = ScoreInfo.of(0, 0);
        var expectedPenaltyScore = ScoreInfo.of(0, 0);
        assertMatchScoreEqual(match, expectedHalfTimeScore, expectedMainScore, expectedPenaltyScore);

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
