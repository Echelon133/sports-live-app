package ml.echelon133.matchservice.event.service;

import ml.echelon133.common.event.dto.MatchEventDetails;
import ml.echelon133.common.event.dto.MatchEventDto;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.match.MatchResult;
import ml.echelon133.common.match.MatchStatus;
import ml.echelon133.common.match.dto.LineupDto;
import ml.echelon133.matchservice.event.exceptions.MatchEventInvalidException;
import ml.echelon133.matchservice.event.model.MatchEvent;
import ml.echelon133.matchservice.event.model.dto.InsertMatchEvent;
import ml.echelon133.matchservice.event.repository.MatchEventRepository;
import ml.echelon133.matchservice.match.model.Match;
import ml.echelon133.matchservice.match.service.MatchService;
import ml.echelon133.matchservice.team.model.TeamPlayer;
import ml.echelon133.matchservice.team.service.TeamPlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ml.echelon133.common.match.MatchStatus.*;

@Service
@Transactional
public class MatchEventService {

    private final MatchService matchService;
    private final TeamPlayerService teamPlayerService;
    private final MatchEventRepository matchEventRepository;
    private final MatchEventWebsocketService matchEventWebsocketService;

    @Autowired
    public MatchEventService(
            MatchService matchService,
            TeamPlayerService teamPlayerService,
            MatchEventRepository matchEventRepository,
            MatchEventWebsocketService matchEventWebsocketService
    ) {
        this.matchService = matchService;
        this.teamPlayerService = teamPlayerService;
        this.matchEventRepository = matchEventRepository;
        this.matchEventWebsocketService = matchEventWebsocketService;
    }

    /**
     * Converts an entity to a dto class that's safe to serialize and display to users.
     *
     * @param event the entity to convert
     * @return dto converted from an entity
     */
    private static MatchEventDto convertEntityToDto(MatchEvent event) {
        return MatchEventDto.from(event.getId(), event.getEvent());
    }

    /**
     * Finds all events of the match with the specified id.
     *
     * @param matchId id of the match whose events will be fetched
     * @return a list of match events
     */
    public List<MatchEventDto> findAllByMatchId(UUID matchId) {
        return matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)
                .stream().map(MatchEventService::convertEntityToDto).collect(Collectors.toList());
    }

    /**
     * Processes a match event and saves it.
     *
     * @param matchId id of the match to which this event belongs to
     * @param eventDto dto containing information about the event
     * @throws ResourceNotFoundException thrown when the match with the provided id does not exist or is marked as deleted
     */
    public void processEvent(UUID matchId, InsertMatchEvent eventDto)
            throws ResourceNotFoundException, MatchEventInvalidException {
        var match = matchService.findEntityById(matchId);
        MatchEvent matchEvent;

        if (eventDto instanceof InsertMatchEvent.StatusDto) {
            matchEvent = processStatusEvent(match, (InsertMatchEvent.StatusDto) eventDto);
        } else if (eventDto instanceof InsertMatchEvent.CommentaryDto) {
            matchEvent = processCommentaryEvent(match, (InsertMatchEvent.CommentaryDto) eventDto);
        } else if (eventDto instanceof InsertMatchEvent.CardDto) {
            matchEvent = processCardEvent(match, (InsertMatchEvent.CardDto) eventDto);
        } else if (eventDto instanceof InsertMatchEvent.GoalDto) {
            matchEvent = processGoalEvent(match, (InsertMatchEvent.GoalDto) eventDto);
        } else if (eventDto instanceof InsertMatchEvent.SubstitutionDto) {
            matchEvent = processSubstitutionEvent(match, (InsertMatchEvent.SubstitutionDto) eventDto);
        } else if (eventDto instanceof InsertMatchEvent.PenaltyDto) {
            matchEvent = processPenaltyEvent(match, (InsertMatchEvent.PenaltyDto) eventDto);
        } else {
            throw new MatchEventInvalidException("handling of this event is not implemented");
        }

        matchEventRepository.save(matchEvent);
        matchEventWebsocketService.sendMatchEvent(
                matchEvent.getMatch().getId(),
                convertEntityToDto(matchEvent)
        );
    }

    /**
     * Processes the status event.
     *
     * Updates the status of the match, unless the target status change is invalid
     * (i.e. it's impossible to finish the match that had never even begun).
     *
     * @param match entity representing the match to which this event is related
     * @param statusDto dto containing information about the event
     * @return match event that is ready to be saved
     */
    private MatchEvent processStatusEvent(Match match, InsertMatchEvent.StatusDto statusDto) throws MatchEventInvalidException {
        // this `MatchStatus.valueOf` should never fail because the status value is pre-validated
        var targetStatus = MatchStatus.valueOf(statusDto.getTargetStatus());
        var validTargetStatuses = VALID_STATUS_CHANGES.get(match.getStatus());

        if (!validTargetStatuses.contains(targetStatus)) {
            throw new MatchEventInvalidException("current status of the match cannot be changed to the requested target status");
        }

        var mainScore = match.getScoreInfo();
        var penaltyScore = match.getPenaltiesInfo();

        // set the final result of the match
        if (targetStatus.equals(FINISHED)) {
            var endResult = MatchResult.NONE;

            if (match.getStatus().equals(PENALTIES)) {
                // a match finishing after penalties cannot end in a draw
                endResult = MatchResult
                        .getResultBasedOnScore(penaltyScore.getHomeGoals(), penaltyScore.getAwayGoals());

                // make sure there is no draw if the game is finishing after the penalties
                if (endResult.equals(MatchResult.DRAW)) {
                    throw new MatchEventInvalidException("match cannot finish after penalties when the score is a draw");
                }
            } else {
                // a match ending after the second half and the extra time is evaluated based on the main score
                endResult = MatchResult
                        .getResultBasedOnScore(mainScore.getHomeGoals(), mainScore.getAwayGoals());

                // make sure there is no draw if the game is finishing after the extra time
                if (match.getStatus().equals(EXTRA_TIME) && endResult.equals(MatchResult.DRAW)) {
                    throw new MatchEventInvalidException("match cannot finish after extra time when the score is a draw");
                }
            }
            match.setResult(endResult);
        }

        var eventDetails = new MatchEventDetails.StatusDto(
                statusDto.getMinute(),
                match.getCompetitionId(),
                targetStatus,
                new MatchEventDetails.SerializedTeamInfo(match.getHomeTeam().getId(), match.getAwayTeam().getId()),
                match.getResult()
        );

        match.setStatus(targetStatus);
        return new MatchEvent(match, eventDetails);
    }

    /**
     * Processes the commentary event.
     *
     * @param match entity representing the match to which this event is related
     * @param commentaryDto dto containing information about the event
     * @return match event that is ready to be saved
     */
    private MatchEvent processCommentaryEvent(Match match, InsertMatchEvent.CommentaryDto commentaryDto) {
        var eventDetails = new MatchEventDetails.CommentaryDto(
                commentaryDto.getMinute(),
                match.getCompetitionId(),
                commentaryDto.getMessage()
        );
        return new MatchEvent(match, eventDetails);
    }

    private static boolean isCardEventOfPlayer(MatchEventDto event, UUID teamPlayerId) {
        if (event.getEvent() instanceof MatchEventDetails.CardDto) {
            var e = (MatchEventDetails.CardDto) event.getEvent();
            return e.getCardedPlayer().getTeamPlayerId().equals(teamPlayerId);
        } else {
            return false;
        }
    }

    private static boolean isCardEventOfCardType(MatchEventDto event, MatchEventDetails.CardDto.CardType... cardTypes) {
        if (event.getEvent() instanceof MatchEventDetails.CardDto) {
            MatchEventDetails.CardDto cDto = (MatchEventDetails.CardDto) event.getEvent();
            var foundCardType= cDto.getCardType();
            return Arrays.asList(cardTypes).contains(foundCardType);
        } else {
            return false;
        }
    }

    /**
     * Processes the card event.
     *
     * @param match entity representing the match to which this event is related
     * @param cardDto dto containing information about the event
     * @return match event that is ready to be saved
     */
    private MatchEvent processCardEvent(Match match, InsertMatchEvent.CardDto cardDto)
            throws MatchEventInvalidException, ResourceNotFoundException {

        throwIfBallNotInPlay(match);

        // this `UUID.fromString` should never fail because the cardedPlayerId is pre-validated
        var cardedTeamPlayer = teamPlayerService.findEntityById(UUID.fromString(cardDto.getCardedPlayerId()));

        // check if the carded player is placed in the lineup of the match
        throwIfPlayerNotInLineup(match, cardedTeamPlayer);

        // initialize the type of the card
        // (if the target color of the card is yellow, for now assume it's the first yellow of the player)
        MatchEventDetails.CardDto.CardType cardType = cardDto
                .isRedCard() ?
                MatchEventDetails.CardDto.CardType.DIRECT_RED :
                MatchEventDetails.CardDto.CardType.YELLOW;

        // fetch all card events of the player to prepare for checking if they can even get yet another card
        var playerCardEvents = this.findAllByMatchId(match.getId()).stream()
                .filter(e -> isCardEventOfPlayer(e, cardedTeamPlayer.getId())).collect(Collectors.toList());

        // only do additional checks if the player had any prior cards
        if (!playerCardEvents.isEmpty()) {
            var secondYellowOrRed = playerCardEvents.stream().anyMatch(
                    e -> isCardEventOfCardType(
                            e,
                            MatchEventDetails.CardDto.CardType.SECOND_YELLOW,
                            MatchEventDetails.CardDto.CardType.DIRECT_RED
                    )
            );

            // throw if the player has had a second yellow or a direct red card because it should not be possible
            // to give them any more cards
            if (secondYellowOrRed) {
                throw new MatchEventInvalidException("the player is already ejected");
            }

            // check if the player has a single yellow card, which is important in case the target card is yellow
            var singleYellow = playerCardEvents.stream().anyMatch(
                    e -> isCardEventOfCardType(e, MatchEventDetails.CardDto.CardType.YELLOW)
            );

            // if the player already has a yellow card and receives another one - mark it as a second yellow,
            // otherwise allow direct red cards even if the player already has a yellow card
            if (singleYellow && !cardDto.isRedCard()) {
                cardType = MatchEventDetails.CardDto.CardType.SECOND_YELLOW;
            }
        }

        var eventDetails = new MatchEventDetails.CardDto(
                cardDto.getMinute(),
                match.getCompetitionId(),
                cardedTeamPlayer.getTeam().getId(),
                cardType,
                new MatchEventDetails.SerializedPlayerInfo(
                    cardedTeamPlayer.getId(),
                    cardedTeamPlayer.getPlayer().getId(),
                    cardedTeamPlayer.getPlayer().getName()
                )
        );
        return new MatchEvent(match, eventDetails);
    }

    /**
     * Processes the goal event.
     *
     * @param match entity representing the match to which this event is related
     * @param goalDto dto containing information about the event
     * @return match event that is ready to be saved
     */
    private MatchEvent processGoalEvent(Match match, InsertMatchEvent.GoalDto goalDto)
            throws MatchEventInvalidException, ResourceNotFoundException {

        throwIfBallNotInPlay(match);

        // this `UUID.fromString` should never fail because the scoringPlayerId is pre-validated
        TeamPlayer scoringPlayer = teamPlayerService.findEntityById(UUID.fromString(goalDto.getScoringPlayerId()));
        // player who scored has to be on the pitch
        throwIfPlayerNotOnPitch(match, scoringPlayer);

        // assisting player is optional, but if it's set, make sure that the player scoring and assisting
        // are from the same team
        TeamPlayer assistingPlayer = null;

        if (goalDto.getAssistingPlayerId() != null) {
            // own goals cannot have assisting players, so reject the event before any checks that require db access
            if (goalDto.isOwnGoal()) {
                throw new MatchEventInvalidException("own goals cannot have a player assisting");
            }

            // this `UUID.fromString` should never fail because the assistingPlayerId is pre-validated when not null
            assistingPlayer = teamPlayerService.findEntityById(UUID.fromString(goalDto.getAssistingPlayerId()));

            // player who assisted has to be on the pitch
            throwIfPlayerNotOnPitch(match, assistingPlayer);

            // make sure that both players - scoring and assisting - are from the same team
            throwIfPlayersPlayForDifferentTeams(scoringPlayer, assistingPlayer);
        }

        // determine the team whose goals should be incremented
        var scoredByHomeTeam = match.getHomeTeam().equals(scoringPlayer.getTeam());
        // flip the scoring side if the goal is an own goal
        if (goalDto.isOwnGoal()) {
            scoredByHomeTeam = !scoredByHomeTeam;
        }
        var scoringTeam = scoredByHomeTeam ? match.getHomeTeam() : match.getAwayTeam();

        MatchEventDetails.SerializedPlayerInfo scoringPlayerInfo =
                new MatchEventDetails.SerializedPlayerInfo(
                        scoringPlayer.getId(),
                        scoringPlayer.getPlayer().getId(),
                        scoringPlayer.getPlayer().getName()
                );

        MatchEventDetails.SerializedPlayerInfo assistingPlayerInfo = null;
        if (assistingPlayer != null) {
            assistingPlayerInfo =
                new MatchEventDetails.SerializedPlayerInfo(
                        assistingPlayer.getId(),
                        assistingPlayer.getPlayer().getId(),
                        assistingPlayer.getPlayer().getName()
                );
        }

        var eventDetails = new MatchEventDetails.GoalDto(
                goalDto.getMinute(),
                match.getCompetitionId(),
                scoringTeam.getId(),
                scoringPlayerInfo,
                assistingPlayerInfo,
                goalDto.isOwnGoal()
        );

        incrementMatchScoreline(match, scoredByHomeTeam);
        return new MatchEvent(match, eventDetails);
    }

    /**
     * Processes the substitution event.
     *
     * @param match entity representing the match to which this event is related
     * @param substitutionDto dto containing information about the event
     * @return match event that is ready to be saved
     */
    private MatchEvent processSubstitutionEvent(Match match, InsertMatchEvent.SubstitutionDto substitutionDto)
            throws MatchEventInvalidException, ResourceNotFoundException {

        throwIfBallNotInPlay(match);

        // this `UUID.fromString` should never fail because the playerInId is pre-validated
        TeamPlayer playerIn = teamPlayerService.findEntityById(UUID.fromString(substitutionDto.getPlayerInId()));
        throwIfPlayerNotInLineup(match, playerIn);

        // this `UUID.fromString` should never fail because the playerOutId is pre-validated
        TeamPlayer playerOut = teamPlayerService.findEntityById(UUID.fromString(substitutionDto.getPlayerOutId()));
        throwIfPlayerNotInLineup(match, playerOut);

        // make sure that both players - in and out - are from the same team
        throwIfPlayersPlayForDifferentTeams(playerIn, playerOut);

        // player can enter the pitch if they:
        //      * started the game on the bench
        //      * have not received double yellow or direct red
        //      * have not been subbed on before
        throwIfPlayerCannotBeSubbedOn(match, playerIn);

        // player can leave the pitch if they:
        //      * have not received double yellow or direct red
        //      * have not been subbed off before
        //      * have been on the pitch from the start or got subbed on during the match
        throwIfPlayerNotOnPitch(match, playerOut);

        MatchEventDetails.SerializedPlayerInfo playerInInfo =
                new MatchEventDetails.SerializedPlayerInfo(
                        playerIn.getId(),
                        playerIn.getPlayer().getId(),
                        playerIn.getPlayer().getName()
                );

        MatchEventDetails.SerializedPlayerInfo playerOutInfo =
                new MatchEventDetails.SerializedPlayerInfo(
                        playerOut.getId(),
                        playerOut.getPlayer().getId(),
                        playerOut.getPlayer().getName()
                );

        var eventDetails = new MatchEventDetails.SubstitutionDto(
                substitutionDto.getMinute(),
                match.getCompetitionId(),
                playerIn.getTeam().getId(),
                playerInInfo,
                playerOutInfo
        );

        return new MatchEvent(match, eventDetails);
    }

    /**
     * Processes the penalty event.
     *
     * @param match entity representing the match to which this event is related
     * @param penaltyDto dto containing information about the event
     * @return match event that is ready to be saved
     */
    private MatchEvent processPenaltyEvent(Match match, InsertMatchEvent.PenaltyDto penaltyDto)
            throws MatchEventInvalidException, ResourceNotFoundException {

        throwIfBallNotInPlay(match);

        // this `UUID.fromString` should never fail because the shootingPlayerId is pre-validated
        TeamPlayer shootingPlayer = teamPlayerService.findEntityById(UUID.fromString(penaltyDto.getShootingPlayerId()));

        // player shooting must be on the pitch
        throwIfPlayerNotOnPitch(match, shootingPlayer);

        // penalties scored during the penalty shootout do not count as goals
        boolean duringPenaltyShootout = match.getStatus().equals(PENALTIES);

        // determine the team whose goals should potentially be incremented
        var scoredByHomeTeam = match.getHomeTeam().equals(shootingPlayer.getTeam());

        // update the scoreline only if the penalty is scored
        if (penaltyDto.isScored()) {
            incrementMatchScoreline(match, scoredByHomeTeam);
        }

        var eventDetails = new MatchEventDetails.PenaltyDto(
                penaltyDto.getMinute(),
                match.getCompetitionId(),
                shootingPlayer.getTeam().getId(),
                new MatchEventDetails.SerializedPlayerInfo(
                        shootingPlayer.getId(),
                        shootingPlayer.getPlayer().getId(),
                        shootingPlayer.getPlayer().getName()
                ),
                !duringPenaltyShootout,
                penaltyDto.isScored()
        );

        return new MatchEvent(match, eventDetails);
    }

    /**
     * Helper method which throws if the status of the match shows that the ball is not in play.
     *
     * This method is useful for preventing the acceptation of events such as goals when the ball in the match
     * is not being played.
     *
     * @param match match whose status will be checked
     * @throws MatchEventInvalidException the exception thrown when the conditions are not met
     */
    private void throwIfBallNotInPlay(Match match) throws MatchEventInvalidException {
        if (!match.getStatus().isBallInPlay()) {
            throw new MatchEventInvalidException("event cannot be processed when the ball is not in play");
        }
    }

    /**
     * Helper method which throws if the player is not in the lineup of a given match.
     *
     * This method is useful for preventing the acceptation of events such as cards when the player who was supposed
     * to receive the card is not even in the lineup (i.e. does not start the game and is not on the bench).
     *
     * @param match match whose lineup will be checked
     * @param teamPlayer player whose presence in the lineup will be checked
     * @throws MatchEventInvalidException the exception thrown when the conditions are not met
     */
    private void throwIfPlayerNotInLineup(Match match, TeamPlayer teamPlayer) throws MatchEventInvalidException {
        var teamPlayerTeam = teamPlayer.getTeam();
        var homeTeam = match.getHomeTeam();
        var awayTeam = match.getAwayTeam();

        if (!(homeTeam.equals(teamPlayerTeam) || awayTeam.equals(teamPlayerTeam))) {
            throw new MatchEventInvalidException(
                    String.format("the player %s does not play for either team", teamPlayer.getId())
            );
        }

        var lineup = matchService.findMatchLineup(match.getId());
        LineupDto.TeamLineup teamLineup = match.getHomeTeam().equals(teamPlayerTeam) ? lineup.getHome() : lineup.getAway();

        var playerInLineup = Stream.concat(
                teamLineup.getStartingPlayers().stream(),
                teamLineup.getSubstitutePlayers().stream()
        ).anyMatch(tPlayer -> tPlayer.getId().equals(teamPlayer.getId()));

        if (!playerInLineup) {
            throw new MatchEventInvalidException(
                    String.format("the player %s is not placed in the lineup of this match", teamPlayer.getId())
            );
        }
    }

    /**
     * Helper method which throws if two {@link TeamPlayer}s do not play for the same team.
     *
     * This method is useful for preventing the acceptation of events such as goals when the player scoring and player
     * assisting are not from the same team.
     *
     * @param player1 first player in the comparison
     * @param player2 second player in the comparison
     * @throws MatchEventInvalidException the exception thrown when the conditions are not met
     */
    private void throwIfPlayersPlayForDifferentTeams(TeamPlayer player1, TeamPlayer player2) throws MatchEventInvalidException {
        if (!player1.getTeam().equals(player2.getTeam())) {
            throw new MatchEventInvalidException("players do not play for the same team");
        }
    }

    /**
     * Helper method which throws if the player is currently not on the pitch.
     *
     * This method is useful for preventing the acceptation of events such as goals when the player who was supposed
     * to score the goal is currently not on the pitch.
     *
     * The player is considered to not be on the pitch if:
     * <ul>
     *     <li>They got sent off (two yellow cards or a red card)</li>
     *     <li>They got substituted off</li>
     *     <li>They either never got substituted on or were not in the starting lineup</li>
     * </ul>
     *
     * @param match match which will be checked
     * @param player player whose current presence on the pitch will be checked
     * @throws MatchEventInvalidException the exception thrown when the conditions are not met
     */
    private void throwIfPlayerNotOnPitch(Match match, TeamPlayer player) throws MatchEventInvalidException {
        var matchEvents = this.findAllByMatchId(match.getId());
        var exception = new MatchEventInvalidException(
                String.format("the player %s is not on the pitch", player.getId())
        );

        boolean sentOff = matchEvents.stream()
                .filter(e -> isCardEventOfPlayer(e, player.getId()))
                .anyMatch(
                        e -> isCardEventOfCardType(
                                e,
                                MatchEventDetails.CardDto.CardType.SECOND_YELLOW,
                                MatchEventDetails.CardDto.CardType.DIRECT_RED
                        )
                );
        if (sentOff) {
            throw exception;
        }

        boolean substitutedOff = matchEvents.stream().anyMatch(
                e -> isSubstitutionOffEventOfPlayer(e, player.getId())
        );
        if (substitutedOff) {
            throw exception;
        }

        var lineup = matchService.findMatchLineup(match.getId());
        LineupDto.TeamLineup teamLineup = match.getHomeTeam().equals(player.getTeam()) ? lineup.getHome() : lineup.getAway();

        boolean substitutedOn = matchEvents.stream().anyMatch(
                e -> isSubstitutionOnEventOfPlayer(e, player.getId()));
        boolean startingPlayer = teamLineup.getStartingPlayers().stream().anyMatch(
                teamPlayer -> teamPlayer.getId().equals(player.getId()));

        if (!(startingPlayer || substitutedOn)) {
            throw exception;
        }
    }

    /**
     * Helper method which throws if the player cannot be substituted on.
     *
     * This method is useful for preventing the acceptation of substitution events in which the player about to enter
     * the pitch cannot do it because of the rules.
     *
     * The player is considered unable to enter the pitch if:
     * <ul>
     *     <li>They began the game on the pitch, therefore they might either already be on the pitch or are unable to enter again.</li>
     *     <li>They got sent off (two yellow cards or a red card)</li>
     *     <li>They have already been substituted on, therefore they cannot enter they pitch if they are already there.</li>
     * </ul>
     *
     * @param match match which will be checked
     * @param player player whose ability to enter the pitch will be checked
     * @throws MatchEventInvalidException the exception thrown when the conditions are not met
     */
    private void throwIfPlayerCannotBeSubbedOn(Match match, TeamPlayer player) throws MatchEventInvalidException {
        var matchEvents = this.findAllByMatchId(match.getId());
        var exception = new MatchEventInvalidException(
                String.format("the player %s cannot enter the pitch", player.getId())
        );

        var lineup = matchService.findMatchLineup(match.getId());
        LineupDto.TeamLineup teamLineup = match.getHomeTeam().equals(player.getTeam()) ? lineup.getHome() : lineup.getAway();

        boolean substitutePlayer= teamLineup.getSubstitutePlayers().stream()
                .anyMatch(teamPlayer -> teamPlayer.getId().equals(player.getId()));
        // a player who started the game on the pitch cannot be subbed on in the game
        if (!substitutePlayer) {
            throw exception;
        }

        boolean sentOff = matchEvents.stream()
                .filter(e -> isCardEventOfPlayer(e, player.getId()))
                .anyMatch(
                        e -> isCardEventOfCardType(
                                e,
                                MatchEventDetails.CardDto.CardType.SECOND_YELLOW,
                                MatchEventDetails.CardDto.CardType.DIRECT_RED
                        )
                );
        // if the player got two yellows or a red while on the bench, they cannot be subbed on
        if (sentOff) {
            throw exception;
        }

        boolean substitutionOn = matchEvents.stream().anyMatch(
                e -> isSubstitutionOnEventOfPlayer(e, player.getId())
        );
        // a player once subbed on cannot be subbed on again
        if (substitutionOn) {
            throw exception;
        }
    }

    /**
     * Predicate that returns `true` if a {@link MatchEventDto} is a substitution event that signifies
     * player with a particular id being substituted off in the match.
     *
     * @param event the event which is being checked
     * @param teamPlayerId the id of the player who is being checked for being substituted off
     * @return `true` if the described conditions are satisfied
     */
    private static boolean isSubstitutionOffEventOfPlayer(MatchEventDto event, UUID teamPlayerId) {
        if (event.getEvent() instanceof MatchEventDetails.SubstitutionDto) {
            var e = (MatchEventDetails.SubstitutionDto) event.getEvent();
            return e.getPlayerOut().getTeamPlayerId().equals(teamPlayerId);
        } else {
            return false;
        }
    }

    /**
     * Predicate that returns `true` if a {@link MatchEventDto} is a substitution event that signifies
     * player with a particular id being substituted on in the match.
     *
     * @param event the event which is being checked
     * @param teamPlayerId the id of the player who is being checked for being substituted on
     * @return `true` if the described conditions are satisfied
     */
    private static boolean isSubstitutionOnEventOfPlayer(MatchEventDto event, UUID teamPlayerId) {
        if (event.getEvent() instanceof MatchEventDetails.SubstitutionDto) {
            var e = (MatchEventDetails.SubstitutionDto) event.getEvent();
            return e.getPlayerIn().getTeamPlayerId().equals(teamPlayerId);
        } else {
            return false;
        }
    }

    /**
     * Helper method which increments the scoreline in the match differently depending on multiple conditions.
     *
     * If the status of the match is:
     * <ul>
     *     <li>FIRST_HALF - increments both the main scoreline and the half-time scoreline</li>
     *     <li>PENALTIES - increments only the penalty scoreline</li>
     *     <li>any other status - increments only the main scoreline</li>
     * </ul>
     *
     * @param match match whose scoreline needs updating
     * @param homeGoal if `true`, increments the home side of the scoreline, otherwise increments the away side
     */
    private void incrementMatchScoreline(Match match, boolean homeGoal) {
        if (match.getStatus().equals(PENALTIES)) {
            var penaltyScore = match.getPenaltiesInfo();
            if (homeGoal) {
                penaltyScore.incrementHomeGoals();
            } else {
                penaltyScore.incrementAwayGoals();
            }
        } else {
            var score = match.getScoreInfo();
            if (homeGoal) {
                score.incrementHomeGoals();
            } else {
                score.incrementAwayGoals();
            }

            // set the half-time score in case we are in the FIRST_HALF
            if (match.getStatus().equals(FIRST_HALF)) {
                match.setHalfTimeScoreInfo(score);
            }
        }
    }
}
