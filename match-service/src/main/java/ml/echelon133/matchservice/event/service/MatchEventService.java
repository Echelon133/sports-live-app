package ml.echelon133.matchservice.event.service;

import ml.echelon133.common.event.dto.MatchEventDetails;
import ml.echelon133.common.event.dto.MatchEventDto;
import ml.echelon133.common.exception.ResourceNotFoundException;
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

import static ml.echelon133.common.match.MatchStatus.FIRST_HALF;
import static ml.echelon133.common.match.MatchStatus.VALID_STATUS_CHANGES;

@Service
@Transactional
public class MatchEventService {

    private final MatchService matchService;
    private final TeamPlayerService teamPlayerService;
    private final MatchEventRepository matchEventRepository;

    @Autowired
    public MatchEventService(MatchService matchService, TeamPlayerService teamPlayerService, MatchEventRepository matchEventRepository) {
        this.matchService = matchService;
        this.teamPlayerService = teamPlayerService;
        this.matchEventRepository = matchEventRepository;
    }

    private static MatchEventDto convertEntityToDto(MatchEvent event) {
        return MatchEventDto.from(event.getId(), event.getEvent());
    }

    private void throwIfBallNotInPlay(Match match) throws MatchEventInvalidException {
        if (!match.getStatus().isBallInPlay()) {
            throw new MatchEventInvalidException("event cannot be processed when the ball is not in play");
        }
    }

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

        if (eventDto instanceof InsertMatchEvent.StatusDto) {
            processStatusEvent(match, (InsertMatchEvent.StatusDto) eventDto);
        } else if (eventDto instanceof InsertMatchEvent.CommentaryDto) {
            processCommentaryEvent(match, (InsertMatchEvent.CommentaryDto) eventDto);
        } else if (eventDto instanceof InsertMatchEvent.CardDto) {
            processCardEvent(match, (InsertMatchEvent.CardDto) eventDto);
        } else if (eventDto instanceof InsertMatchEvent.GoalDto) {
            processGoalEvent(match, (InsertMatchEvent.GoalDto) eventDto);
        } else {
            throw new MatchEventInvalidException("handling of this event is not implemented");
        }
    }

    /**
     * Processes the status event and saves it.
     *
     * Updates the status of the match, unless the target status change is invalid
     * (i.e. it's impossible to finish the match that had never even begun).
     *
     * @param match entity representing the match to which this event is related
     * @param statusDto dto containing information about the event
     */
    private void processStatusEvent(Match match, InsertMatchEvent.StatusDto statusDto) throws MatchEventInvalidException {
        // this `MatchStatus.valueOf` should never fail because the status value is pre-validated
        var targetStatus = MatchStatus.valueOf(statusDto.getTargetStatus());
        var validTargetStatuses = VALID_STATUS_CHANGES.get(match.getStatus());

        if (!validTargetStatuses.contains(targetStatus)) {
            throw new MatchEventInvalidException("current status of the match cannot be changed to the requested target status");
        }

        var eventDetails = new MatchEventDetails.StatusDto(
                statusDto.getMinute(),
                match.getCompetitionId(),
                targetStatus
        );
        match.setStatus(targetStatus);
        matchEventRepository.save(new MatchEvent(match, eventDetails));
    }

    /**
     * Processes the commentary event and saves it.
     *
     * @param match entity representing the match to which this event is related
     * @param commentaryDto dto containing information about the event
     */
    private void processCommentaryEvent(Match match, InsertMatchEvent.CommentaryDto commentaryDto) {
        var eventDetails = new MatchEventDetails.CommentaryDto(
                commentaryDto.getMinute(),
                match.getCompetitionId(),
                commentaryDto.getMessage()
        );
        matchEventRepository.save(new MatchEvent(match, eventDetails));
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
        MatchEventDetails.CardDto cDto = (MatchEventDetails.CardDto) event.getEvent();
        var foundCardType= cDto.getCardType();
        return Arrays.asList(cardTypes).contains(foundCardType);
    }

    /**
     * Processes the card event and saves it.
     *
     * @param match entity representing the match to which this event is related
     * @param cardDto dto containing information about the event
     */
    private void processCardEvent(Match match, InsertMatchEvent.CardDto cardDto)
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
        matchEventRepository.save(new MatchEvent(match, eventDetails));
    }

    /**
     * Processes the goal event and saves it.
     *
     * @param match entity representing the match to which this event is related
     * @param goalDto dto containing information about the event
     */
    private void processGoalEvent(Match match, InsertMatchEvent.GoalDto goalDto)
            throws MatchEventInvalidException, ResourceNotFoundException {

        throwIfBallNotInPlay(match);

        // this `UUID.fromString` should never fail because the scoringPlayerId is pre-validated
        TeamPlayer scoringPlayer = teamPlayerService.findEntityById(UUID.fromString(goalDto.getScoringPlayerId()));
        // player who scored has to be in the match's lineup
        throwIfPlayerNotInLineup(match, scoringPlayer);

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

            // player who assisted has to be in the match's lineup
            throwIfPlayerNotInLineup(match, assistingPlayer);

            // make sure that both players - scoring and assisting - are from the same team
            if (!scoringPlayer.getTeam().equals(assistingPlayer.getTeam())) {
                throw new MatchEventInvalidException("players do not play for the same team");
            }
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

        var scoreInfo = match.getScoreInfo();

        if (scoredByHomeTeam) {
            scoreInfo.incrementHomeGoals();
        } else {
            scoreInfo.incrementAwayGoals();
        }

        // set the half-time score in case we are in the FIRST_HALF
        if (match.getStatus().equals(FIRST_HALF)) {
            match.setHalfTimeScoreInfo(scoreInfo);
        }

        matchEventRepository.save(new MatchEvent(match, eventDetails));
    }
}
