package pl.echelon133.competitionservice.competition.service;

import ml.echelon133.common.event.KafkaTopicNames;
import ml.echelon133.common.event.dto.MatchEventDetails;
import ml.echelon133.common.event.dto.SerializedPlayer;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.match.MatchResult;
import ml.echelon133.common.match.MatchStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.echelon133.competitionservice.competition.model.Competition;
import pl.echelon133.competitionservice.competition.model.PlayerStats;
import pl.echelon133.competitionservice.competition.model.TeamStats;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchEventDetailsMessageListenerTests {

    @Mock
    private PlayerStatsService playerStatsService;

    @Mock
    private TeamStatsService teamStatsService;

    @InjectMocks
    private MatchEventDetailsMessageListener matchEventDetailsMessageListener;

    private ConsumerRecord<UUID, MatchEventDetails> createTestConsumerRecord(int offset, MatchEventDetails matchEvent) {
        return new ConsumerRecord<>(
                KafkaTopicNames.MATCH_EVENTS,
                0,
                offset,
                UUID.randomUUID(),
                matchEvent
        );
    }

    private static class TeamStatsMatcher implements ArgumentMatcher<TeamStats> {
        private TeamStats expectedStats;

        public TeamStatsMatcher(TeamStats expectedStats) {
            this.expectedStats = expectedStats;
        }

        @Override
        public boolean matches(TeamStats teamStats) {
            return teamStats.getTeamId() == expectedStats.getTeamId() &&
                    teamStats.getMatchesPlayed() == expectedStats.getMatchesPlayed() &&
                    teamStats.getWins() == expectedStats.getWins() &&
                    teamStats.getDraws() == expectedStats.getDraws() &&
                    teamStats.getLosses() == expectedStats.getLosses() &&
                    teamStats.getGoalsScored() == expectedStats.getGoalsScored() &&
                    teamStats.getGoalsConceded() == expectedStats.getGoalsConceded() &&
                    teamStats.getPoints() == expectedStats.getPoints();
        }
    }

    private static class PlayerStatsMatcher implements ArgumentMatcher<PlayerStats> {
        private PlayerStats expectedStats;

        public PlayerStatsMatcher(PlayerStats expectedStats) {
            this.expectedStats = expectedStats;
        }

        @Override
        public boolean matches(PlayerStats playerStats) {
            return playerStats.getGoals() == expectedStats.getGoals() &&
                    playerStats.getAssists() == expectedStats.getAssists() &&
                    playerStats.getYellowCards() == expectedStats.getYellowCards() &&
                    playerStats.getRedCards() == expectedStats.getRedCards();
        }
    }

    private List<ConsumerRecord<UUID, MatchEventDetails>> createTestConsumerRecords(MatchEventDetails ...matchEvents) {
        List<ConsumerRecord<UUID, MatchEventDetails>> records = new ArrayList<>(matchEvents.length);
        for (int offset = 0; offset < matchEvents.length; offset++) {
            records.add(createTestConsumerRecord(offset, matchEvents[offset]));
        }
        return records;
    }

    private void assertNoOperation() {
        verify(playerStatsService, never()).save(any());
        verify(teamStatsService, never()).saveAll(any());
    }

    @Test
    @DisplayName("onMessage does nothing when received match events do not require any database operations by design")
    public void onMessage_NoOperationMatchEvents_NoDatabaseOperations() {
        // match events which do not require any database updates
        var noopEvents = List.of(
                // Commentary events do not contain any information about teams/players
                new MatchEventDetails.CommentaryDto("1", null, "test"),
                // all non-"FINISHED" Status events do not cause database updates
                new MatchEventDetails.StatusDto("1", null, MatchStatus.NOT_STARTED, null, null, null),
                new MatchEventDetails.StatusDto("1", null, MatchStatus.FIRST_HALF, null, null, null),
                new MatchEventDetails.StatusDto("1", null, MatchStatus.HALF_TIME, null, null, null),
                new MatchEventDetails.StatusDto("1", null, MatchStatus.SECOND_HALF, null, null, null),
                new MatchEventDetails.StatusDto("1", null, MatchStatus.EXTRA_TIME, null, null, null),
                new MatchEventDetails.StatusDto("1", null, MatchStatus.PENALTIES, null, null, null),
                new MatchEventDetails.StatusDto("1", null, MatchStatus.ABANDONED, null, null, null),
                new MatchEventDetails.StatusDto("1", null, MatchStatus.POSTPONED, null, null, null),
                // Penalty events describing missed penalties do not cause database updates
                new MatchEventDetails.PenaltyDto("1", null, null, null, true, false),
                // Penalty events describing scored penalties (but during the penalty shootout) do not cause database updates
                new MatchEventDetails.PenaltyDto("1", null, null, null, false, true),
                // Substitution events do not cause database updates
                new MatchEventDetails.SubstitutionDto("1", null, null, null, null)
        );
        var noopConsumerRecords= createTestConsumerRecords(
                noopEvents.toArray(MatchEventDetails[]::new)
        );

        // when
        for (var record : noopConsumerRecords) {
            matchEventDetailsMessageListener.onMessage(record);
        }

        // then
        assertNoOperation();
    }

    @Test
    @DisplayName("onMessage (StatsEvent) updates team stats of both teams when a match finishes with a draw")
    public void onMessage_StatusEventMatchDrawn_UpdatesBothTeamStats() throws ResourceNotFoundException {
        UUID homeTeamId = UUID.randomUUID();
        var homeTeamStats = new TeamStats(homeTeamId, null, null);
        UUID awayTeamId = UUID.randomUUID();
        var awayTeamStats = new TeamStats(awayTeamId, null, null);
        UUID competitionId = UUID.randomUUID();
        int homeGoals = 3;
        int awayGoals = 3;

        var expectedHomeTeamStats = new TeamStats(homeTeamId, null, null);
        expectedHomeTeamStats.incrementMatchesPlayed();
        expectedHomeTeamStats.incrementDraws();
        expectedHomeTeamStats.incrementPointsBy(1);
        expectedHomeTeamStats.incrementGoalsScoredBy(homeGoals);
        expectedHomeTeamStats.incrementGoalsConcededBy(awayGoals);

        var expectedAwayTeamStats = new TeamStats(awayTeamId, null, null);
        expectedAwayTeamStats.incrementMatchesPlayed();
        expectedAwayTeamStats.incrementDraws();
        expectedAwayTeamStats.incrementPointsBy(1);
        expectedAwayTeamStats.incrementGoalsScoredBy(awayGoals);
        expectedAwayTeamStats.incrementGoalsConcededBy(homeGoals);

        var record = createTestConsumerRecord(0,
                new MatchEventDetails.StatusDto(
                        "90",
                        competitionId,
                        MatchStatus.FINISHED,
                        new MatchEventDetails.SerializedTeamInfo(homeTeamId, awayTeamId),
                        MatchResult.DRAW,
                        new MatchEventDetails.SerializedScoreInfo(homeGoals, awayGoals)
                )
        );

        // given
        given(teamStatsService.findTeamStats(homeTeamId, competitionId)).willReturn(homeTeamStats);
        given(teamStatsService.findTeamStats(awayTeamId, competitionId)).willReturn(awayTeamStats);

        // when
        matchEventDetailsMessageListener.onMessage(record);

        // then
        verify(teamStatsService).saveAll(argThat(s -> {
            var iter = s.iterator();
            var homeStats = iter.next();
            var awayStats = iter.next();
            return new TeamStatsMatcher(expectedHomeTeamStats).matches(homeStats) &&
                    new TeamStatsMatcher(expectedAwayTeamStats).matches(awayStats);
        }));
    }

    @Test
    @DisplayName("onMessage (StatusEvent) updates team stats of both teams when a match finishes with a home win")
    public void onMessage_StatusEventMatchHomeWin_UpdatesBothTeamStats() throws ResourceNotFoundException {
        UUID homeTeamId = UUID.randomUUID();
        var homeTeamStats = new TeamStats(homeTeamId, null, null);
        UUID awayTeamId = UUID.randomUUID();
        var awayTeamStats = new TeamStats(awayTeamId, null, null);
        UUID competitionId = UUID.randomUUID();
        int homeGoals = 4;
        int awayGoals = 3;

        var expectedHomeTeamStats = new TeamStats(homeTeamId, null, null);
        expectedHomeTeamStats.incrementMatchesPlayed();
        expectedHomeTeamStats.incrementWins();
        expectedHomeTeamStats.incrementPointsBy(3);
        expectedHomeTeamStats.incrementGoalsScoredBy(homeGoals);
        expectedHomeTeamStats.incrementGoalsConcededBy(awayGoals);

        var expectedAwayTeamStats = new TeamStats(awayTeamId, null, null);
        expectedAwayTeamStats.incrementMatchesPlayed();
        expectedAwayTeamStats.incrementLosses();
        expectedAwayTeamStats.incrementGoalsScoredBy(awayGoals);
        expectedAwayTeamStats.incrementGoalsConcededBy(homeGoals);

        var record = createTestConsumerRecord(0,
                new MatchEventDetails.StatusDto(
                        "90",
                        competitionId,
                        MatchStatus.FINISHED,
                        new MatchEventDetails.SerializedTeamInfo(homeTeamId, awayTeamId),
                        MatchResult.HOME_WIN,
                        new MatchEventDetails.SerializedScoreInfo(homeGoals, awayGoals)
                )
        );

        // given
        given(teamStatsService.findTeamStats(homeTeamId, competitionId)).willReturn(homeTeamStats);
        given(teamStatsService.findTeamStats(awayTeamId, competitionId)).willReturn(awayTeamStats);

        // when
        matchEventDetailsMessageListener.onMessage(record);

        // then
        verify(teamStatsService).saveAll(argThat(s -> {
            var iter = s.iterator();
            var homeStats = iter.next();
            var awayStats = iter.next();
            return new TeamStatsMatcher(expectedHomeTeamStats).matches(homeStats) &&
                    new TeamStatsMatcher(expectedAwayTeamStats).matches(awayStats);
        }));
    }

    @Test
    @DisplayName("onMessage (StatusEvent) updates team stats of both teams when a match finishes with an away win")
    public void onMessage_StatusEventMatchAwayWin_UpdatesBothTeamStats() throws ResourceNotFoundException {
        UUID homeTeamId = UUID.randomUUID();
        var homeTeamStats = new TeamStats(homeTeamId, null, null);
        UUID awayTeamId = UUID.randomUUID();
        var awayTeamStats = new TeamStats(awayTeamId, null, null);
        UUID competitionId = UUID.randomUUID();
        int homeGoals = 3;
        int awayGoals = 4;

        var expectedHomeTeamStats = new TeamStats(homeTeamId, null, null);
        expectedHomeTeamStats.incrementMatchesPlayed();
        expectedHomeTeamStats.incrementLosses();
        expectedHomeTeamStats.incrementGoalsScoredBy(homeGoals);
        expectedHomeTeamStats.incrementGoalsConcededBy(awayGoals);

        var expectedAwayTeamStats = new TeamStats(awayTeamId, null, null);
        expectedAwayTeamStats.incrementMatchesPlayed();
        expectedAwayTeamStats.incrementWins();
        expectedAwayTeamStats.incrementPointsBy(3);
        expectedAwayTeamStats.incrementGoalsScoredBy(awayGoals);
        expectedAwayTeamStats.incrementGoalsConcededBy(homeGoals);

        var record = createTestConsumerRecord(0,
                new MatchEventDetails.StatusDto(
                        "90",
                        competitionId,
                        MatchStatus.FINISHED,
                        new MatchEventDetails.SerializedTeamInfo(homeTeamId, awayTeamId),
                        MatchResult.AWAY_WIN,
                        new MatchEventDetails.SerializedScoreInfo(homeGoals, awayGoals)
                )
        );

        // given
        given(teamStatsService.findTeamStats(homeTeamId, competitionId)).willReturn(homeTeamStats);
        given(teamStatsService.findTeamStats(awayTeamId, competitionId)).willReturn(awayTeamStats);

        // when
        matchEventDetailsMessageListener.onMessage(record);

        // then
        verify(teamStatsService).saveAll(argThat(s -> {
            var iter = s.iterator();
            var homeStats = iter.next();
            var awayStats = iter.next();
            return new TeamStatsMatcher(expectedHomeTeamStats).matches(homeStats) &&
                    new TeamStatsMatcher(expectedAwayTeamStats).matches(awayStats);
        }));
    }

    @Test
    @DisplayName("onMessage (StatusEvent) does nothing when a finished match has result set to NONE")
    public void onMessage_StatusEventMatchResultIsNone_NoDatabaseOperations() {
        UUID homeTeamId = UUID.randomUUID();
        UUID awayTeamId = UUID.randomUUID();
        UUID competitionId = UUID.randomUUID();
        int homeGoals = 3;
        int awayGoals = 3;

        var record = createTestConsumerRecord(0,
                new MatchEventDetails.StatusDto(
                        "90",
                        competitionId,
                        MatchStatus.FINISHED,
                        new MatchEventDetails.SerializedTeamInfo(homeTeamId, awayTeamId),
                        MatchResult.NONE, // can only happen as a business logic error
                        new MatchEventDetails.SerializedScoreInfo(homeGoals, awayGoals)
                )
        );

        // when
        matchEventDetailsMessageListener.onMessage(record);

        // then
        assertNoOperation();
    }

    @Test
    @DisplayName("onMessage (StatusEvent) does nothing when the home team is not found in the database")
    public void onMessage_StatusEventHomeTeamNotFound_NoDatabaseOperations() throws ResourceNotFoundException {
        UUID homeTeamId = UUID.randomUUID();
        UUID awayTeamId = UUID.randomUUID();
        UUID competitionId = UUID.randomUUID();
        int homeGoals = 3;
        int awayGoals = 3;

        var record = createTestConsumerRecord(0,
                new MatchEventDetails.StatusDto(
                        "90",
                        competitionId,
                        MatchStatus.FINISHED,
                        new MatchEventDetails.SerializedTeamInfo(homeTeamId, awayTeamId),
                        MatchResult.DRAW,
                        new MatchEventDetails.SerializedScoreInfo(homeGoals, awayGoals)
                )
        );

        // given
        given(teamStatsService.findTeamStats(homeTeamId, competitionId))
                .willThrow(new ResourceNotFoundException(TeamStats.class, homeTeamId));

        // when
        matchEventDetailsMessageListener.onMessage(record);

        // then
        assertNoOperation();
    }

    @Test
    @DisplayName("onMessage (StatusEvent) does nothing when the away team is not found in the database")
    public void onMessage_StatusEventAwayTeamNotFound_NoDatabaseOperations() throws ResourceNotFoundException {
        UUID homeTeamId = UUID.randomUUID();
        UUID awayTeamId = UUID.randomUUID();
        UUID competitionId = UUID.randomUUID();
        int homeGoals = 3;
        int awayGoals = 3;

        var record = createTestConsumerRecord(0,
                new MatchEventDetails.StatusDto(
                        "90",
                        competitionId,
                        MatchStatus.FINISHED,
                        new MatchEventDetails.SerializedTeamInfo(homeTeamId, awayTeamId),
                        MatchResult.DRAW,
                        new MatchEventDetails.SerializedScoreInfo(homeGoals, awayGoals)
                )
        );

        // given
        given(teamStatsService.findTeamStats(homeTeamId, competitionId)).willReturn(new TeamStats());
        given(teamStatsService.findTeamStats(awayTeamId, competitionId))
                .willThrow(new ResourceNotFoundException(TeamStats.class, awayTeamId));

        // when
        matchEventDetailsMessageListener.onMessage(record);

        // then
        assertNoOperation();
    }

    @Test
    @DisplayName("onMessage (StatusEvent) does nothing when the competition is not found in the database")
    public void onMessage_StatusEventCompetitionNotFound_NoDatabaseOperations() throws ResourceNotFoundException {
        UUID homeTeamId = UUID.randomUUID();
        UUID awayTeamId = UUID.randomUUID();
        UUID competitionId = UUID.randomUUID();
        int homeGoals = 3;
        int awayGoals = 3;

        var record = createTestConsumerRecord(0,
                new MatchEventDetails.StatusDto(
                        "90",
                        competitionId,
                        MatchStatus.FINISHED,
                        new MatchEventDetails.SerializedTeamInfo(homeTeamId, awayTeamId),
                        MatchResult.DRAW,
                        new MatchEventDetails.SerializedScoreInfo(homeGoals, awayGoals)
                )
        );

        // given
        given(teamStatsService.findTeamStats(homeTeamId, competitionId)).willReturn(new TeamStats());
        given(teamStatsService.findTeamStats(awayTeamId, competitionId))
                .willThrow(new ResourceNotFoundException(Competition.class, competitionId));

        // when
        matchEventDetailsMessageListener.onMessage(record);

        // then
        assertNoOperation();
    }

    @Test
    @DisplayName("onMessage (CardEvent) increments the yellow card count of a player when they get a yellow card")
    public void onMessage_CardEventYellowCard_IncrementsPlayerYellowCards() throws ResourceNotFoundException {
        var teamId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var playerName = "Test Name";

        var playerStats = new PlayerStats(playerId, teamId, playerName);

        var expectedPlayerStats = new PlayerStats(playerId, teamId, playerName);
        expectedPlayerStats.incrementYellowCards();

        var record = createTestConsumerRecord(0,
                new MatchEventDetails.CardDto(
                        "1",
                        competitionId,
                        teamId,
                        MatchEventDetails.CardDto.CardType.YELLOW,
                        new SerializedPlayer(
                                null, playerId, playerName
                        )
                )
        );

        // given
        given(playerStatsService.findPlayerStatsOrDefault(playerId, competitionId, teamId, playerName))
                .willReturn(playerStats);

        // when
        matchEventDetailsMessageListener.onMessage(record);

        // then
        verify(playerStatsService).save(argThat(new PlayerStatsMatcher(expectedPlayerStats)));
    }

    @Test
    @DisplayName("onMessage (CardEvent) increments the red card count of a player they get a second yellow card")
    public void onMessage_CardEventSecondYellowCard_IncrementsPlayerRedCards() throws ResourceNotFoundException {
        var teamId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var playerName = "Test Name";

        var playerStats = new PlayerStats(playerId, teamId, playerName);

        var expectedPlayerStats = new PlayerStats(playerId, teamId, playerName);
        expectedPlayerStats.incrementRedCards();

        var record = createTestConsumerRecord(0,
                new MatchEventDetails.CardDto(
                        "1",
                        competitionId,
                        teamId,
                        MatchEventDetails.CardDto.CardType.SECOND_YELLOW,
                        new SerializedPlayer(
                                null, playerId, playerName
                        )
                )
        );

        // given
        given(playerStatsService.findPlayerStatsOrDefault(playerId, competitionId, teamId, playerName))
                .willReturn(playerStats);

        // when
        matchEventDetailsMessageListener.onMessage(record);

        // then
        verify(playerStatsService).save(argThat(new PlayerStatsMatcher(expectedPlayerStats)));
    }

    @Test
    @DisplayName("onMessage (CardEvent) increments the red card count of a player when they get a red card")
    public void onMessage_CardEventRedCard_IncrementsPlayerRedCards() throws ResourceNotFoundException {
        var teamId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var playerName = "Test Name";

        var playerStats = new PlayerStats(playerId, teamId, playerName);

        var expectedPlayerStats = new PlayerStats(playerId, teamId, playerName);
        expectedPlayerStats.incrementRedCards();

        var record = createTestConsumerRecord(0,
                new MatchEventDetails.CardDto(
                        "1",
                        competitionId,
                        teamId,
                        MatchEventDetails.CardDto.CardType.DIRECT_RED,
                        new SerializedPlayer(
                                null, playerId, playerName
                        )
                )
        );

        // given
        given(playerStatsService.findPlayerStatsOrDefault(playerId, competitionId, teamId, playerName))
                .willReturn(playerStats);

        // when
        matchEventDetailsMessageListener.onMessage(record);

        // then
        verify(playerStatsService).save(argThat(new PlayerStatsMatcher(expectedPlayerStats)));
    }

    @Test
    @DisplayName("onMessage (CardEvent) does nothing when the competition is not found in the database")
    public void onMessage_CardEventCompetitionNotFound_NoDatabaseOperations() throws ResourceNotFoundException {
        var teamId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var playerName = "Test Name";

        var record = createTestConsumerRecord(0,
                new MatchEventDetails.CardDto(
                        "1",
                        competitionId,
                        teamId,
                        MatchEventDetails.CardDto.CardType.DIRECT_RED,
                        new SerializedPlayer(
                                null, playerId, playerName
                        )
                )
        );

        // given
        given(playerStatsService.findPlayerStatsOrDefault(playerId, competitionId, teamId, playerName))
                .willThrow(new ResourceNotFoundException(Competition.class, competitionId));

        // when
        matchEventDetailsMessageListener.onMessage(record);

        // then
        assertNoOperation();
    }

    @Test
    @DisplayName("onMessage (GoalEvent) does nothing when a player scores an own goal")
    public void onMessage_GoalEventPlayerScoresOwnGoal_NoDatabaseOperations() throws ResourceNotFoundException {
        var teamId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var playerName = "Test Name";

        var record = createTestConsumerRecord(0,
                new MatchEventDetails.GoalDto(
                        "1",
                        competitionId,
                        teamId,
                        new SerializedPlayer(
                                null, playerId, playerName
                        ),
                        null,
                        true
                )
        );

        // when
        matchEventDetailsMessageListener.onMessage(record);

        // then
        assertNoOperation();
    }

    @Test
    @DisplayName("onMessage (GoalEvent) increments the goal count of a player when they score a goal")
    public void onMessage_GoalEventPlayerScoresGoalNoAssist_IncrementsPlayerGoals() throws ResourceNotFoundException {
        var teamId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var playerName = "Test Name";

        var playerStats = new PlayerStats(playerId, teamId, playerName);

        var expectedPlayerStats = new PlayerStats(playerId, teamId, playerName);
        expectedPlayerStats.incrementGoals();

        var record = createTestConsumerRecord(0,
                new MatchEventDetails.GoalDto(
                        "1",
                        competitionId,
                        teamId,
                        new SerializedPlayer(
                                null, playerId, playerName
                        ),
                        null,
                        false
                )
        );

        // given
        given(playerStatsService.findPlayerStatsOrDefault(playerId, competitionId, teamId, playerName))
                .willReturn(playerStats);

        // when
        matchEventDetailsMessageListener.onMessage(record);

        // then
        verify(playerStatsService).save(argThat(new PlayerStatsMatcher(expectedPlayerStats)));
    }

    @Test
    @DisplayName("onMessage (GoalEvent) increments the goal count and the assist count of players involved in a goal")
    public void onMessage_GoalEventPlayerScoresGoalWithAssist_IncrementsPlayerGoalsAndAssists() throws ResourceNotFoundException {
        var teamId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();

        var scoringPlayerId = UUID.randomUUID();
        var scoringPlayerName = "Test Name";

        var assistingPlayerId = UUID.randomUUID();
        var assistingPlayerName = "Test Name";

        var scoringPlayerStats = new PlayerStats(scoringPlayerId, teamId, scoringPlayerName);
        var expectedScoringPlayerStats = new PlayerStats(scoringPlayerId, teamId, scoringPlayerName);
        expectedScoringPlayerStats.incrementGoals();

        var assistingPlayerStats = new PlayerStats(assistingPlayerId, teamId, assistingPlayerName);
        var expectedAssistingPlayerStats = new PlayerStats(scoringPlayerId, teamId, scoringPlayerName);
        expectedAssistingPlayerStats.incrementAssists();

        var record = createTestConsumerRecord(0,
                new MatchEventDetails.GoalDto(
                        "1",
                        competitionId,
                        teamId,
                        new SerializedPlayer(
                                null, scoringPlayerId, scoringPlayerName
                        ),
                        new SerializedPlayer(
                                null, assistingPlayerId, assistingPlayerName
                        ),
                        false
                )
        );

        // given
        given(playerStatsService.findPlayerStatsOrDefault(scoringPlayerId, competitionId, teamId, scoringPlayerName))
                .willReturn(scoringPlayerStats);
        given(playerStatsService.findPlayerStatsOrDefault(assistingPlayerId, competitionId, teamId, assistingPlayerName))
                .willReturn(assistingPlayerStats);

        // when
        matchEventDetailsMessageListener.onMessage(record);

        // then
        verify(playerStatsService, atLeastOnce()).save(
                argThat(new PlayerStatsMatcher(expectedScoringPlayerStats))
        );
        verify(playerStatsService, atLeastOnce()).save(
                argThat(new PlayerStatsMatcher(expectedAssistingPlayerStats))
        );
    }

    @Test
    @DisplayName("onMessage (GoalEvent) does nothing when the competition is not found in the database")
    public void onMessage_GoalEventCompetitionNotFound_NoDatabaseOperations() throws ResourceNotFoundException {
        var teamId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var playerName = "Test Name";

        var record = createTestConsumerRecord(0,
                new MatchEventDetails.GoalDto(
                        "1",
                        competitionId,
                        teamId,
                        new SerializedPlayer(
                                null, playerId, playerName
                        ),
                        null,
                        false
                )
        );

        // given
        given(playerStatsService.findPlayerStatsOrDefault(playerId, competitionId, teamId, playerName))
                .willThrow(new ResourceNotFoundException(Competition.class, competitionId));

        // when
        matchEventDetailsMessageListener.onMessage(record);

        // then
        assertNoOperation();
    }

    @Test
    @DisplayName("onMessage (PenaltyEvent) increments the goal count of a player when they score a penalty")
    public void onMessage_PenaltyEventPlayerScores_IncrementsPlayerGoals() throws ResourceNotFoundException {
        var teamId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var playerName = "Test Name";

        var playerStats = new PlayerStats(playerId, teamId, playerName);

        var expectedPlayerStats = new PlayerStats(playerId, teamId, playerName);
        expectedPlayerStats.incrementGoals();

        var record = createTestConsumerRecord(0,
                new MatchEventDetails.PenaltyDto(
                        "1",
                        competitionId,
                        teamId,
                        new SerializedPlayer(
                                null, playerId, playerName
                        ),
                        true,
                        true
                )
        );

        // given
        given(playerStatsService.findPlayerStatsOrDefault(playerId, competitionId, teamId, playerName))
                .willReturn(playerStats);

        // when
        matchEventDetailsMessageListener.onMessage(record);

        // then
        verify(playerStatsService).save(argThat(new PlayerStatsMatcher(expectedPlayerStats)));
    }

    @Test
    @DisplayName("onMessage (PenaltyEvent) does nothing when the competition is not found in the database")
    public void onMessage_PenaltyEventCompetitionNotFound_NoDatabaseOperations() throws ResourceNotFoundException {
        var teamId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var playerName = "Test Name";

        var record = createTestConsumerRecord(0,
                new MatchEventDetails.PenaltyDto(
                        "1",
                        competitionId,
                        teamId,
                        new SerializedPlayer(
                                null, playerId, playerName
                        ),
                        true,
                        true
                )
        );

        // given
        given(playerStatsService.findPlayerStatsOrDefault(playerId, competitionId, teamId, playerName))
                .willThrow(new ResourceNotFoundException(Competition.class, competitionId));

        // when
        matchEventDetailsMessageListener.onMessage(record);

        // then
        assertNoOperation();
    }
}
