package pl.echelon133.competitionservice.competition.service;

import jakarta.transaction.Transactional;
import ml.echelon133.common.event.dto.*;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.match.MatchResult;
import ml.echelon133.common.match.MatchStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.MessageListener;
import pl.echelon133.competitionservice.competition.model.PlayerStats;

import java.util.List;
import java.util.UUID;

@Transactional
public class MatchEventDetailsMessageListener implements MessageListener<UUID, MatchEventDetails> {

    private final Logger logger = LoggerFactory.getLogger(MatchEventDetailsMessageListener.class);
    private final PlayerStatsService playerStatsService;
    private final TeamStatsService teamStatsService;

    public MatchEventDetailsMessageListener(PlayerStatsService playerStatsService, TeamStatsService teamStatsService) {
        this.playerStatsService = playerStatsService;
        this.teamStatsService = teamStatsService;

        logger.info(
                "Initialized with PlayerStatsService {} and TeamStatsService {}",
                playerStatsService, teamStatsService
        );
    }

    @Override
    public void onMessage(@NotNull ConsumerRecord<UUID, MatchEventDetails> record) {
        logger.debug("Received record {}", record.key());

        var recordId = record.key();
        var event = record.value();

        try {
            switch (event) {
                case StatusEventDetailsDto statusEvent -> processStatusEvent(recordId, statusEvent);
                case CardEventDetailsDto cardEvent -> processCardEvent(recordId, cardEvent);
                case GoalEventDetailsDto goalEvent -> processGoalEvent(recordId, goalEvent);
                case PenaltyEventDetailsDto penaltyEvent -> processPenaltyEvent(recordId, penaltyEvent);
                default -> logNoOperation(recordId, "The event does not contain any data about teams or players");
            }
        } catch (ResourceNotFoundException ex) {
            logProcessingFailure(recordId, ex.getMessage());
        }
    }

    /**
     * Processes {@link ml.echelon133.common.event.dto.StatusEventDetailsDto} events received from Kafka.
     *
     * Its task is to update team stats when the match ends. This includes:
     * <ul>
     *     <li>incrementing the number of matches played by both teams</li>
     *     <li>changing the number of points of both teams based on the result</li>
     *     <li>incrementing the number of wins/draws/losses of both teams based on the result</li>
     *     <li>changing the number of goals scored/conceded by both teams based on the result</li>
     * </ul>
     *
     * @param recordId id of the received record
     * @param event match event containing information about a status event happening in a match
     */
    private void processStatusEvent(UUID recordId, StatusEventDetailsDto event) throws ResourceNotFoundException {
        if (!event.targetStatus().equals(MatchStatus.FINISHED)) {
            logNoOperation(recordId, "Match status other than `FINISHED` does not require any action");
            return;
        }

        MatchResult result = event.result();

        // if the match status is FINISHED, all match results except for NONE are valid, so
        // if we encounter NONE here, it means that the service which has produced this event
        // must have broken some invariant, therefore this event cannot be handled properly
        if (result.equals(MatchResult.NONE)) {
            logProcessingFailure(
                    recordId,
                    "The match is `FINISHED`, but its result is set to `NONE`, which is unexpected"
            );
            return;
        }

        var teamInfo = event.teams();
        var homeTeamId = teamInfo.homeTeamId();
        var awayTeamId = teamInfo.awayTeamId();
        var competitionId = event.competitionId();

        var home = teamStatsService.findTeamStats(homeTeamId, competitionId);
        var away = teamStatsService.findTeamStats(awayTeamId, competitionId);

        // increment `matchesPlayed` of both teams
        home.incrementMatchesPlayed();
        away.incrementMatchesPlayed();

        // update `goalsScored` and `goalsConceded` of both teams
        var homeGoals = event.mainScore().homeGoals();
        var awayGoals = event.mainScore().awayGoals();
        home.incrementGoalsScoredBy(homeGoals);
        home.incrementGoalsConcededBy(awayGoals);
        away.incrementGoalsScoredBy(awayGoals);
        away.incrementGoalsConcededBy(homeGoals);

        // based on the result:
        //      * give 3 points for wins
        //      * give 1 point for draws
        //      * give 0 points for losses
        switch (result) {
            case HOME_WIN -> {
                home.incrementWins();
                away.incrementLosses();
                home.incrementPointsBy(3);
            }
            case AWAY_WIN -> {
                home.incrementLosses();
                away.incrementWins();
                away.incrementPointsBy(3);
            }
            case DRAW -> {
                home.incrementDraws();
                away.incrementDraws();
                home.incrementPointsBy(1);
                away.incrementPointsBy(1);
            }
            // unreachable, because any event with match result set to `NONE` is rejected at the top
            default -> {
            }
        }
        logger.debug(
                "Record {} increments team stats of teams {} and {} in a competition {}",
                recordId, homeTeamId, awayTeamId, competitionId
        );
        teamStatsService.saveAll(List.of(home, away));
    }

    /**
     * Processes {@link ml.echelon133.common.event.dto.CardEventDetailsDto} events received from Kafka.
     *
     * Its task is to update the number of player's yellow/red cards in a particular competition.
     *
     * @param recordId id of the received record
     * @param event match event containing information about a card event happening in a match
     */
    private void processCardEvent(UUID recordId, CardEventDetailsDto event) throws ResourceNotFoundException {
        var cardedPlayer = event.cardedPlayer();
        var competitionId = event.competitionId();
        var playerId = cardedPlayer.playerId();

        PlayerStats playerStats = playerStatsService.findPlayerStatsOrDefault(
                playerId,
                competitionId,
                event.teamId(),
                cardedPlayer.name()
        );

        CardEventDetailsDto.CardType t = event.cardType();
        String cardType = "";

        switch (t) {
            case YELLOW -> {
                playerStats.incrementYellowCards();
                cardType = "yellow";
            }
            case SECOND_YELLOW, DIRECT_RED -> {
                playerStats.incrementRedCards();
                cardType = "red";
            }
        }

        logger.debug(
                "Record {} increments {} cards of player {} in competition {}",
                recordId, cardType, playerId, competitionId
        );
        playerStatsService.save(playerStats);
    }

    /**
     * Processes {@link ml.echelon133.common.event.dto.GoalEventDetailsDto} events received from Kafka.
     *
     * Its task is to update the number of goals/assists for players involved in a goal in a particular competition.
     * Own goals are ignored, because they do not count as actual goals in statistics.
     *
     * @param recordId id of the received record
     * @param event match event containing information about a goal event happening in a match
     */
    private void processGoalEvent(UUID recordId, GoalEventDetailsDto event) throws ResourceNotFoundException {
        if (event.ownGoal()) {
            logNoOperation(recordId, "Own goals do not impact player's goal statistics");
            return;
        }
        var competitionId = event.competitionId();

        var scoringPlayer = event.scoringPlayer();
        var scoringPlayerId = scoringPlayer.playerId();
        PlayerStats scoringPlayerStats = playerStatsService.findPlayerStatsOrDefault(
                scoringPlayerId,
                competitionId,
                event.teamId(),
                scoringPlayer.name()
        );
        scoringPlayerStats.incrementGoals();
        logger.debug(
                "Record {} increments goals of player {} in competition {}",
                recordId, scoringPlayerId, competitionId
        );
        playerStatsService.save(scoringPlayerStats);

        // the goal might have an assisting player
        if (event.assistingPlayer() != null) {
            var assistingPlayer = event.assistingPlayer();
            var assistingPlayerId = assistingPlayer.playerId();
            PlayerStats assistingPlayerStats = playerStatsService.findPlayerStatsOrDefault(
                    assistingPlayerId,
                    competitionId,
                    event.teamId(),
                    assistingPlayer.name()
            );
            assistingPlayerStats.incrementAssists();
            logger.debug(
                    "Record {} increments assists of player {} in competition {}",
                    recordId, assistingPlayerId, competitionId
            );
            playerStatsService.save(assistingPlayerStats);
        }
    }

    /**
     * Processes {@link ml.echelon133.common.event.dto.PenaltyEventDetailsDto} events received from Kafka.
     *
     * Its task is to update the number of goals for a player who scored a penalty in a particular competition.
     * Missed penalties, or penalties scored in a penalty shootout are ignored.
     *
     * @param recordId id of the received record
     * @param event match event containing information about a penalty event happening in a match
     */
    private void processPenaltyEvent(UUID recordId, PenaltyEventDetailsDto event) throws ResourceNotFoundException {
        if (!event.countAsGoal()) {
            logNoOperation(recordId, "Penalty during the penalty shootout does not count as an actual goal");
            return;
        }

        if (!event.scored()) {
            logNoOperation(recordId, "Penalty missed");
            return;
        }

        var competitionId = event.competitionId();
        var shootingPlayer = event.shootingPlayer();
        var shootingPlayerId = shootingPlayer.playerId();
        PlayerStats shootingPlayerStats = playerStatsService.findPlayerStatsOrDefault(
                shootingPlayerId,
                competitionId,
                event.teamId(),
                shootingPlayer.name()
        );
        shootingPlayerStats.incrementGoals();
        logger.debug(
                "Record {} increments goals of player {} in competition {}",
                recordId, shootingPlayerId, competitionId
        );
        playerStatsService.save(shootingPlayerStats);
    }

    private void logNoOperation(UUID recordId, String reason) {
        logger.debug("Record {} did not cause any database updates. Reason: \"{}\"", recordId, reason);
    }

    private void logProcessingFailure(UUID recordId, String reason) {
        logger.error("Record {} could not be handled. Reason: \"{}\"", recordId, reason);
    }
}
