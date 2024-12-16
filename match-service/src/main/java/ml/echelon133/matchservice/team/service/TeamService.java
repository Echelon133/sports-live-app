package ml.echelon133.matchservice.team.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.match.MatchResult;
import ml.echelon133.matchservice.coach.service.CoachService;
import ml.echelon133.matchservice.team.model.*;
import ml.echelon133.matchservice.team.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final CoachService coachService;

    @Autowired
    public TeamService(TeamRepository teamRepository, CoachService coachService) {
        this.teamRepository = teamRepository;
        this.coachService = coachService;
    }

    /**
     * Returns the information about the team with specified id.
     *
     * @param id id of the team
     * @return a dto representing the team
     * @throws ResourceNotFoundException thrown when the team does not exist in the database
     */
    public TeamDto findById(UUID id) throws ResourceNotFoundException {
        return teamRepository
                .findTeamById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Team.class, id));
    }

    /**
     * Returns the entity representing a team with the specified id.
     * @param id id of the team's entity
     * @return team's entity
     * @throws ResourceNotFoundException thrown when the team does not exist in the database or is deleted
     */
    public Team findEntityById(UUID id) throws ResourceNotFoundException {
        return teamRepository
                .findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Team.class, id));
    }

    /**
     * Updates the team's information.
     *
     * The values in {@link UpsertTeamDto} have to be pre-validated before being used here,
     * otherwise incorrect data will be placed into the database.
     *
     * @param id id of the team to update
     * @param teamDto dto containing updated information about the team
     * @return a dto representing the updated team
     * @throws ResourceNotFoundException thrown when the team/country/coach does not exist in the database
     */
    public TeamDto updateTeam(UUID id, UpsertTeamDto teamDto) throws ResourceNotFoundException {
        var teamToUpdate = teamRepository
                .findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Team.class, id));

        teamToUpdate.setName(teamDto.name());
        teamToUpdate.setCrestUrl(teamDto.crestUrl());

        teamToUpdate.setCountryCode(teamDto.countryCode());

        // this `UUID.fromString` should never fail because the CoachId value is pre-validated
        var coachId = UUID.fromString(teamDto.coachId());
        var coach = coachService.findEntityById(coachId);
        teamToUpdate.setCoach(coach);

        return TeamMapper.entityToDto(teamRepository.save(teamToUpdate));
    }

    /**
     * Creates the team's entry in the database.
     *
     * The values in {@link UpsertTeamDto} have to be pre-validated before being used here,
     * otherwise incorrect data will be placed into the database.
     *
     * @param teamDto dto representing the information about a team that will be saved in the database
     * @return a dto representing the newly saved team
     * @throws ResourceNotFoundException thrown when the team's country or coach does not exist in the database
     */
    public TeamDto createTeam(UpsertTeamDto teamDto) throws ResourceNotFoundException {
        // this `UUID.fromString` should never fail because the CoachId value is pre-validated
        var coachId = UUID.fromString(teamDto.coachId());
        var coach = coachService.findEntityById(coachId);

        var team = new Team(teamDto.name(), teamDto.crestUrl(), teamDto.countryCode(), coach);
        return TeamMapper.entityToDto(teamRepository.save(team));
    }

    /**
     * Finds all teams whose names contain the specified phrase.
     *
     * @param phrase phrase which needs to appear in the name of the team
     * @param pageable information about the wanted page
     * @return a page of teams which match the filter
     */
    public Page<TeamDto> findTeamsByName(String phrase, Pageable pageable) {
        return teamRepository.findAllByNameContaining(phrase, pageable);
    }

    /**
     * Finds all teams whose ids are in the id list.
     *
     * @param teamIds requested team ids
     * @param pageable information about the wanted page
     * @return a page of teams
     */
    public Page<TeamDto> findTeamsByIds(List<UUID> teamIds, Pageable pageable) {
        return teamRepository.findAllByTeamIds(teamIds, pageable);
    }

    /**
     * Marks a team with the specified id as deleted.
     *
     * @param id id of the team to be marked as deleted
     * @return how many entities have been affected
     */
    public Integer markTeamAsDeleted(UUID id)  {
        return teamRepository.markTeamAsDeleted(id);
    }

    /**
     * Evaluates the form of a team in a particular competition based on the last 5 matches of that team.
     *
     * @param teamId id of the team whose form is being evaluated
     * @param competitionId id of the competition from which the last 5 matches will be taken
     * @return a list of at most 5 matches (each one of them evaluated by team's form)
     */
    public List<TeamFormDto> evaluateForm(UUID teamId, UUID competitionId) {
        return teamRepository.findFormEvaluationMatches(teamId, competitionId).stream()
                .map(matchDetails ->
                        new TeamFormDto(interpretResultAsSymbol(teamId, matchDetails), matchDetails)
                ).collect(Collectors.toList());
    }

    /**
     * Evaluates the general form of a team based on the last 5 matches of that team (in all competitions).
     *
     * @param teamId id of the team whose form is being evaluated
     * @return a list of at most 5 matches (each one of them evaluated by team's form)
     */
    public List<TeamFormDto> evaluateGeneralForm(UUID teamId) {
        return teamRepository.findGeneralFormEvaluationMatches(teamId).stream()
                .map(matchDetails ->
                        new TeamFormDto(interpretResultAsSymbol(teamId, matchDetails), matchDetails)
                ).collect(Collectors.toList());
    }

    /**
     * Helper method which interprets the result of the match from the perspective of a particular team.
     *
     * @param teamId id of the team from whose perspective to interpret the result
     * @param matchDetails details describing a finished match and its results
     * @return a single character ('W', 'L', 'D', '?')
     */
    private static char interpretResultAsSymbol(UUID teamId, TeamFormDetailsDto matchDetails) {
        var matchResult = matchDetails.getResult();
        char symbol;

        if (matchResult.equals(MatchResult.NONE)) {
            symbol = '?';
        } else if (matchResult.equals(MatchResult.DRAW)) {
            symbol = 'D';
        } else {
            // either HOME_WIN or AWAY_WIN
            var homeWin = matchResult.equals(MatchResult.HOME_WIN) && matchDetails.getHomeTeam().id().equals(teamId);
            var awayWin = matchResult.equals(MatchResult.AWAY_WIN) && matchDetails.getAwayTeam().id().equals(teamId);
            if (homeWin || awayWin) {
                symbol = 'W';
            } else {
                symbol = 'L';
            }
        }
        return symbol;
    }
}
