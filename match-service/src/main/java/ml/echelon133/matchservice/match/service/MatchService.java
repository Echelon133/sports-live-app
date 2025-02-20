package ml.echelon133.matchservice.match.service;

import jakarta.transaction.Transactional;
import ml.echelon133.common.constants.DateFormatConstants;
import ml.echelon133.common.event.KafkaTopicNames;
import ml.echelon133.common.event.dto.MatchInfo;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.match.MatchStatus;
import ml.echelon133.matchservice.match.exceptions.LineupPlayerInvalidException;
import ml.echelon133.matchservice.match.model.*;
import ml.echelon133.matchservice.match.repository.MatchRepository;
import ml.echelon133.matchservice.referee.service.RefereeService;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.team.model.TeamPlayer;
import ml.echelon133.matchservice.team.model.TeamPlayerDto;
import ml.echelon133.matchservice.team.service.TeamPlayerService;
import ml.echelon133.matchservice.team.service.TeamService;
import ml.echelon133.matchservice.venue.service.VenueService;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MatchService {

    public static final String DATE_OF_MATCH_FORMAT = DateFormatConstants.DATE_TIME_FORMAT;
    public static final DateTimeFormatter DATE_OF_MATCH_FORMATTER = DateTimeFormatter.ofPattern(DATE_OF_MATCH_FORMAT);

    private final TeamService teamService;
    private final TeamPlayerService teamPlayerService;
    private final VenueService venueService;
    private final RefereeService refereeService;
    private final MatchRepository matchRepository;
    private final KafkaProducer<UUID, MatchInfo> matchInfoKafkaProducer;

    @Autowired
    public MatchService(TeamService teamService,
                        TeamPlayerService teamPlayerService,
                        VenueService venueService,
                        RefereeService refereeService,
                        MatchRepository matchRepository,
                        KafkaProducer<UUID, MatchInfo> matchInfoKafkaProducer
    ) {
        this.teamService = teamService;
        this.teamPlayerService = teamPlayerService;
        this.venueService = venueService;
        this.refereeService = refereeService;
        this.matchRepository = matchRepository;
        this.matchInfoKafkaProducer = matchInfoKafkaProducer;
    }

    public Match findEntityById(UUID id) throws ResourceNotFoundException {
        return matchRepository
                .findById(id)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Match.class, id));
    }

    /**
     * Returns the information about the match with specified id.
     *
     * @param id id of the match
     * @return a dto representing the match
     * @throws ResourceNotFoundException thrown when the match does not exist in the database
     */
    public MatchDto findById(UUID id) throws ResourceNotFoundException {
        return matchRepository
                .findMatchById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Match.class, id));
    }

    /**
     * Marks a match with the specified id as deleted.
     *
     * @param id id of the match to be marked as deleted
     * @return how many entities have been affected
     */
    public Integer markMatchAsDeleted(UUID id)  {
        return matchRepository.markMatchAsDeleted(id);
    }

    /**
     * Creates the match's entry in the database.
     *
     * The values in {@link UpsertMatchDto} have to be pre-validated before being used here,
     * otherwise incorrect data will be placed into the database.
     *
     * @param matchDto dto representing the information about a match that will be saved in the database
     * @return a dto representing the newly saved match
     * @throws ResourceNotFoundException thrown when any entity that the match consists of does not exist in the database
     */
    public MatchDto createMatch(UpsertMatchDto matchDto) throws ResourceNotFoundException {
        var match = new Match();

        // this `UUID.fromString` should never fail because the homeTeamId value is pre-validated
        var homeTeamId = UUID.fromString(matchDto.homeTeamId());
        var homeTeam = teamService.findEntityById(homeTeamId);
        match.setHomeTeam(homeTeam);

        // this `UUID.fromString` should never fail because the awayTeamId value is pre-validated
        var awayTeamId = UUID.fromString(matchDto.awayTeamId());
        var awayTeam = teamService.findEntityById(awayTeamId);
        match.setAwayTeam(awayTeam);

        // this `LocalDateTime.parse` should never fail because the startTimeUTC value is pre-validated
        var startTimeUTC = LocalDateTime.parse(matchDto.startTimeUTC(), DATE_OF_MATCH_FORMATTER);
        match.setStartTimeUTC(startTimeUTC);

        // this `UUID.fromString` should never fail because the venueId value is pre-validated
        var venueId = UUID.fromString(matchDto.venueId());
        var venue = venueService.findEntityById(venueId);
        match.setVenue(venue);

        // referee is optional, only fetch it if it's not null
        if (matchDto.refereeId() != null) {
            // this `UUID.fromString` should never fail because the refereeId value is pre-validated
            var refereeId = UUID.fromString(matchDto.refereeId());
            var referee = refereeService.findEntityById(refereeId);
            match.setReferee(referee);
        }

        // this `UUID.fromString` should never fail because the competitionId value is pre-validated
        var competitionId = UUID.fromString(matchDto.competitionId());
        match.setCompetitionId(competitionId);

        var savedMatch = matchRepository.save(match);

        // let other services know about this match being created for that particular competition,
        // so that it can be assigned to a round (in case of a league competition),
        // or a stage (in case of a knockout competition)
        matchInfoKafkaProducer.send(
            new ProducerRecord<>(
                    KafkaTopicNames.MATCH_INFO,
                    savedMatch.getId(),
                    new MatchInfo.CreationEvent(savedMatch.getCompetitionId(), savedMatch.getId())
            )
        );

        return MatchMapper.entityToDto(savedMatch);
    }

    /**
     * Updates the match's entry in the database.
     *
     * The values in {@link UpsertMatchDto} have to be pre-validated before being used here,
     * otherwise incorrect data will be placed into the database.
     *
     * @param matchId id of the match to update
     * @param matchDto dto containing updated information about the match
     * @return a dto representing the updated match
     * @throws ResourceNotFoundException thrown when any entity that the match consists of does not exist in the database
     */
    public MatchDto updateMatch(UUID matchId, UpsertMatchDto matchDto) throws ResourceNotFoundException {
        var matchToUpdate = findEntityById(matchId);

        // this `UUID.fromString` should never fail because the homeTeamId value is pre-validated
        var homeTeamId = UUID.fromString(matchDto.homeTeamId());
        var homeTeam = teamService.findEntityById(homeTeamId);
        matchToUpdate.setHomeTeam(homeTeam);

        // this `UUID.fromString` should never fail because the awayTeamId value is pre-validated
        var awayTeamId = UUID.fromString(matchDto.awayTeamId());
        var awayTeam = teamService.findEntityById(awayTeamId);
        matchToUpdate.setAwayTeam(awayTeam);

        // this `LocalDateTime.parse` should never fail because the startTimeUTC value is pre-validated
        var startTimeUTC = LocalDateTime.parse(matchDto.startTimeUTC(), DATE_OF_MATCH_FORMATTER);
        matchToUpdate.setStartTimeUTC(startTimeUTC);

        // this `UUID.fromString` should never fail because the venueId value is pre-validated
        var venueId = UUID.fromString(matchDto.venueId());
        var venue = venueService.findEntityById(venueId);
        matchToUpdate.setVenue(venue);

        // referee is optional, only fetch it if it's not null
        if (matchDto.refereeId() != null) {
            // this `UUID.fromString` should never fail because the refereeId value is pre-validated
            var refereeId = UUID.fromString(matchDto.refereeId());
            var referee = refereeService.findEntityById(refereeId);
            matchToUpdate.setReferee(referee);
        }

        // this `UUID.fromString` should never fail because the competitionId value is pre-validated
        var competitionId = UUID.fromString(matchDto.competitionId());
        matchToUpdate.setCompetitionId(competitionId);

        return MatchMapper.entityToDto(matchRepository.save(matchToUpdate));
    }

    /**
     * Finds all matches that happen on the specified date in client's time zone and groups them by the
     * id of their competition.
     *
     * <p>Example - representation of 2023/01/01 in different time zones</p>
     * <table><thead><tr><th>ZoneOffset</th><th>Start (UTC)</th><th>End (UTC)<br></th></tr></thead><tbody><tr><td>00:00</td><td>2023/01/01 00:00</td><td>2023/01/01 23:59</td></tr><tr><td>+01:00</td><td>2022/12/31 23:00</td><td>2023/01/01 22:59</td></tr><tr><td>-08:00</td><td>2023/01/01 08:00</td><td>2023/01/02 07:59<br></td></tr></tbody></table>*
     *
     * @param date specifies the day of the match
     * @param zoneOffset specifies the difference between the client's time zone and the UTC
     * @param pageable information about the wanted page
     * @return lists of matches happening on a particular day in client's time zone,
     *      grouped by the id of their competition
     */
    public Map<UUID, List<CompactMatchDto>> findMatchesByDate(LocalDate date, ZoneOffset zoneOffset, Pageable pageable) {
        LocalDateTime clientLocalMidnight = LocalDateTime.of(date, LocalTime.MIDNIGHT);
        // represent the midnight in client's time zone in UTC
        LocalDateTime startUTC = LocalDateTime.ofInstant(
                clientLocalMidnight.toInstant(zoneOffset),
                ZoneOffset.UTC
        );
        // represent the end of the day in client's time zone in UTC
        LocalDateTime endUTC = startUTC
                .plusHours(23)
                .plusMinutes(59);
        return matchRepository
                .findAllBetween(startUTC, endUTC, pageable)
                .stream().collect(Collectors.groupingBy(CompactMatchDto::getCompetitionId));
    }

    /**
     * Finds all matches of a certain team and filters them by their status.
     *
     * @param teamId id of the team whose matches we want to fetch
     * @param matchFinished if `true`, only returns matches that are finished and have a final result
     * @param pageable information about the wanted page
     * @return lists of matches of a particular team that are filtered based on the status of the match
     */
    public List<CompactMatchDto> findMatchesByTeam(UUID teamId, boolean matchFinished, Pageable pageable) {
        List<String> acceptedStatuses;
        if (matchFinished) {
            // only fetch matches that are finished
            acceptedStatuses = MatchStatus.RESULT_TYPE_STATUSES;
        } else {
            // only fetch matches that are not finished
            acceptedStatuses = MatchStatus.FIXTURE_TYPE_STATUSES;
        }
        return matchRepository.findAllByTeamIdAndStatuses(teamId, acceptedStatuses, pageable);
    }

    /**
     * Finds the lineup of the match with the specified id.
     * @param matchId id of the match whose lineup will be fetched
     * @return the lineup of the match
     */
    public LineupDto findMatchLineup(UUID matchId) {
        var formations = matchRepository
                .findLineupFormationsByMatchId(matchId)
                .orElse(LineupFormationsDto.from(null, null));
        var homeLineup = new LineupDto.TeamLineup(
                matchRepository.findHomeStartingPlayersByMatchId(matchId),
                matchRepository.findHomeSubstitutePlayersByMatchId(matchId),
                formations.getHomeFormation()
        );
        var awayLineup = new LineupDto.TeamLineup(
                matchRepository.findAwayStartingPlayersByMatchId(matchId),
                matchRepository.findAwaySubstitutePlayersByMatchId(matchId),
                formations.getAwayFormation()
        );
        return new LineupDto(homeLineup, awayLineup);
    }

    /**
     * Updates the home lineup of the match with the specified id.
     *
     * The values in {@link UpsertLineupDto} have to be pre-validated before being used here,
     * otherwise incorrect data will be placed into the database.
     *
     * @param matchId id of the match whose home lineup is to be updated
     * @param lineupDto dto containing updated information about match's home lineup
     * @throws ResourceNotFoundException thrown when the match with the specified id does not exist in the
     *      database or is marked as deleted
     * @throws LineupPlayerInvalidException thrown when at least one player to be placed in the lineup actually exists
     *      but does not play for the particular team
     */
    public void updateHomeLineup(UUID matchId, UpsertLineupDto lineupDto)
            throws ResourceNotFoundException, LineupPlayerInvalidException {
        updateLineup(matchId, lineupDto, true);
    }

    /**
     * Updates the away lineup of the match with the specified id.
     *
     * The values in {@link UpsertLineupDto} have to be pre-validated before being used here,
     * otherwise incorrect data will be placed into the database.
     *
     * @param matchId id of the match whose away lineup is to be updated
     * @param lineupDto dto containing updated information about match's away lineup
     * @throws ResourceNotFoundException thrown when the match with the specified id does not exist in the
     *      database or is marked as deleted
     * @throws LineupPlayerInvalidException thrown when at least one player to be placed in the lineup actually exists
     *      but does not play for the particular team
     */
    public void updateAwayLineup(UUID matchId, UpsertLineupDto lineupDto)
            throws ResourceNotFoundException, LineupPlayerInvalidException {
        updateLineup(matchId, lineupDto, false);
    }

    /**
     * Updates either lineup of the match with the specified id.
     *
     * The values in {@link UpsertLineupDto} have to be pre-validated before being used here,
     * otherwise incorrect data will be placed into the database.
     *
     * @param matchId id of the match whose lineup is to be updated
     * @param lineupDto dto containing updated information about match's lineup
     * @param homeLineup flag which signifies whether the lineup to be updated is the home lineup
     * @throws ResourceNotFoundException thrown when the match with the specified id does not exist in the
     *      database or is marked as deleted
     * @throws LineupPlayerInvalidException thrown when at least one player to be placed in the lineup actually exists
     *      but does not play for the particular team
     */
    private void updateLineup(UUID matchId, UpsertLineupDto lineupDto, boolean homeLineup)
            throws ResourceNotFoundException, LineupPlayerInvalidException {
        var match = findEntityById(matchId);

        Team team = homeLineup ? match.getHomeTeam() : match.getAwayTeam();

        Set<UUID> validTeamPlayerIds = teamPlayerService.findAllPlayersOfTeam(team.getId())
                .stream().map(TeamPlayerDto::getId).collect(Collectors.toSet());

        // UUID.fromString should never fail because the validation of UpsertLineupDto guarantees that every single
        // string representing ids of starting players is a valid uuid
        List<UUID> wantedStartingPlayers = lineupDto.startingPlayers().stream()
                .map(UUID::fromString).collect(Collectors.toList());
        List<TeamPlayer> startingPlayers;
        if (validTeamPlayerIds.containsAll(wantedStartingPlayers)) {
            // at this point we know that these ids belong to existing team players who play for that team,
            // therefore we can just turn them into references to TeamPlayer entities
            startingPlayers = teamPlayerService.mapAllIdsToReferences(wantedStartingPlayers);
        } else {
            throw new LineupPlayerInvalidException(true);
        }

        // UUID.fromString should never fail because the validation of UpsertLineupDto guarantees that every single
        // string representing ids of substitute players is a valid uuid
        List<UUID> wantedSubstitutePlayers = lineupDto.substitutePlayers().stream()
                .map(UUID::fromString).collect(Collectors.toList());
        List<TeamPlayer> substitutePlayers;
        if (validTeamPlayerIds.containsAll(wantedSubstitutePlayers)) {
            // at this point we know that these ids belong to existing team players who play for that team,
            // therefore we can just turn them into references to TeamPlayer entities
            substitutePlayers = teamPlayerService.mapAllIdsToReferences(wantedSubstitutePlayers);
        } else {
            throw new LineupPlayerInvalidException(false);
        }

        Lineup lineup = homeLineup ? match.getHomeLineup() : match.getAwayLineup();
        lineup.setStartingPlayers(startingPlayers);
        lineup.setSubstitutePlayers(substitutePlayers);
        lineup.setFormation(lineupDto.formation());
        matchRepository.save(match);
    }

    /**
     * Finds all matches whose ids are in the id list.
     *
     * @param matchIds requested match ids
     * @return a list of matches
     */
    public List<CompactMatchDto> findMatchesByIds(List<UUID> matchIds) {
        return matchRepository.findAllByMatchIds(matchIds);
    }
}
