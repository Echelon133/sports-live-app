package ml.echelon133.matchservice.team.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.team.dto.TeamDto;
import ml.echelon133.matchservice.coach.service.CoachService;
import ml.echelon133.matchservice.country.service.CountryService;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.team.model.UpsertTeamDto;
import ml.echelon133.matchservice.team.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.UUID;

@Service
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final CountryService countryService;
    private final CoachService coachService;

    @Autowired
    public TeamService(TeamRepository teamRepository, CountryService countryService, CoachService coachService) {
        this.teamRepository = teamRepository;
        this.countryService = countryService;
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

        teamToUpdate.setName(teamDto.getName());
        teamToUpdate.setCrestUrl(teamDto.getCrestUrl());

        // this `UUID.fromString` should never fail because the CountryId value is pre-validated
        var countryId = UUID.fromString(teamDto.getCountryId());
        var country = countryService.findEntityById(countryId);
        teamToUpdate.setCountry(country);

        // this `UUID.fromString` should never fail because the CoachId value is pre-validated
        var coachId = UUID.fromString(teamDto.getCoachId());
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
        // this `UUID.fromString` should never fail because the CountryId value is pre-validated
        var countryId = UUID.fromString(teamDto.getCountryId());
        var country = countryService.findEntityById(countryId);

        // this `UUID.fromString` should never fail because the CoachId value is pre-validated
        var coachId = UUID.fromString(teamDto.getCoachId());
        var coach = coachService.findEntityById(coachId);

        var team = new Team(teamDto.getName(), teamDto.getCrestUrl(), country, coach);
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
     * Marks a team with the specified id as deleted.
     *
     * @param id id of the team to be marked as deleted
     * @return how many entities have been affected
     */
    public Integer markTeamAsDeleted(UUID id)  {
        return teamRepository.markTeamAsDeleted(id);
    }
}
