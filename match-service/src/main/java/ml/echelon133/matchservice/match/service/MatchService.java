package ml.echelon133.matchservice.match.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.match.dto.MatchDto;
import ml.echelon133.common.match.dto.MatchStatusDto;
import ml.echelon133.matchservice.match.model.Match;
import ml.echelon133.matchservice.match.model.UpsertMatchDto;
import ml.echelon133.matchservice.match.repository.MatchRepository;
import ml.echelon133.matchservice.referee.model.Referee;
import ml.echelon133.matchservice.referee.repository.RefereeRepository;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.team.repository.TeamRepository;
import ml.echelon133.matchservice.venue.model.Venue;
import ml.echelon133.matchservice.venue.repository.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Transactional
public class MatchService {

    public static final String DATE_OF_MATCH_FORMAT = "yyyy/MM/d H:m";
    public static final DateTimeFormatter DATE_OF_MATCH_FORMATTER = DateTimeFormatter.ofPattern(DATE_OF_MATCH_FORMAT);

    // ONLY USE IT FOR READING DATA
    private final TeamRepository teamRepository;

    // ONLY USE IT FOR READING DATA
    private final VenueRepository venueRepository;

    // ONLY USE IT FOR READING DATA
    private final RefereeRepository refereeRepository;

    private final MatchRepository matchRepository;

    @Autowired
    public MatchService(TeamRepository teamRepository, VenueRepository venueRepository, RefereeRepository refereeRepository, MatchRepository matchRepository) {
        this.teamRepository = teamRepository;
        this.venueRepository = venueRepository;
        this.refereeRepository = refereeRepository;
        this.matchRepository = matchRepository;
    }

    private Match findMatchEntityById(UUID id) throws ResourceNotFoundException {
        return matchRepository
                .findById(id)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Match.class, id));
    }

    // TODO: consider moving this to TeamService
    private Team findTeamEntityById(UUID id) throws ResourceNotFoundException {
        return teamRepository
                .findById(id)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Team.class, id));
    }

    // TODO: consider moving this to VenueService
    private Venue findVenueEntityById(UUID id) throws ResourceNotFoundException {
        return venueRepository
                .findById(id)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Venue.class, id));
    }

    // TODO: consider moving this to RefereeService
    private Referee findRefereeEntityById(UUID id) throws ResourceNotFoundException {
        return refereeRepository
                .findById(id)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Referee.class, id));
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
     * Returns the status of the match with specified id.
     *
     * @param id id of the match
     * @return a dto representing the match's status
     * @throws ResourceNotFoundException thrown when the match does not exist in the database
     */
    public MatchStatusDto findStatusById(UUID id) throws ResourceNotFoundException {
        return matchRepository
                .findMatchStatusById(id)
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
        var homeTeamId = UUID.fromString(matchDto.getHomeTeamId());
        var homeTeam = findTeamEntityById(homeTeamId);
        match.setHomeTeam(homeTeam);

        // this `UUID.fromString` should never fail because the awayTeamId value is pre-validated
        var awayTeamId = UUID.fromString(matchDto.getAwayTeamId());
        var awayTeam = findTeamEntityById(awayTeamId);
        match.setAwayTeam(awayTeam);

        // this `LocalDateTime.parse` should never fail because the startTimeUTC value is pre-validated
        var startTimeUTC = LocalDateTime.parse(matchDto.getStartTimeUTC(), DATE_OF_MATCH_FORMATTER);
        match.setStartTimeUTC(startTimeUTC);

        // this `UUID.fromString` should never fail because the venueId value is pre-validated
        var venueId = UUID.fromString(matchDto.getVenueId());
        var venue = findVenueEntityById(venueId);
        match.setVenue(venue);

        // referee is optional, only fetch it if it's not null
        if (matchDto.getRefereeId() != null) {
            // this `UUID.fromString` should never fail because the refereeId value is pre-validated
            var refereeId = UUID.fromString(matchDto.getRefereeId());
            var referee = findRefereeEntityById(refereeId);
            match.setReferee(referee);
        }

        // this `UUID.fromString` should never fail because the competitionId value is pre-validated
        var competitionId = UUID.fromString(matchDto.getCompetitionId());
        match.setCompetitionId(competitionId);

        return MatchMapper.entityToDto(matchRepository.save(match));
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
        var matchToUpdate = findMatchEntityById(matchId);

        // this `UUID.fromString` should never fail because the homeTeamId value is pre-validated
        var homeTeamId = UUID.fromString(matchDto.getHomeTeamId());
        var homeTeam = findTeamEntityById(homeTeamId);
        matchToUpdate.setHomeTeam(homeTeam);

        // this `UUID.fromString` should never fail because the awayTeamId value is pre-validated
        var awayTeamId = UUID.fromString(matchDto.getAwayTeamId());
        var awayTeam = findTeamEntityById(awayTeamId);
        matchToUpdate.setAwayTeam(awayTeam);

        // this `LocalDateTime.parse` should never fail because the startTimeUTC value is pre-validated
        var startTimeUTC = LocalDateTime.parse(matchDto.getStartTimeUTC(), DATE_OF_MATCH_FORMATTER);
        matchToUpdate.setStartTimeUTC(startTimeUTC);

        // this `UUID.fromString` should never fail because the venueId value is pre-validated
        var venueId = UUID.fromString(matchDto.getVenueId());
        var venue = findVenueEntityById(venueId);
        matchToUpdate.setVenue(venue);

        // referee is optional, only fetch it if it's not null
        if (matchDto.getRefereeId() != null) {
            // this `UUID.fromString` should never fail because the refereeId value is pre-validated
            var refereeId = UUID.fromString(matchDto.getRefereeId());
            var referee = findRefereeEntityById(refereeId);
            matchToUpdate.setReferee(referee);
        }

        // this `UUID.fromString` should never fail because the competitionId value is pre-validated
        var competitionId = UUID.fromString(matchDto.getCompetitionId());
        matchToUpdate.setCompetitionId(competitionId);

        return MatchMapper.entityToDto(matchRepository.save(matchToUpdate));
    }
}
