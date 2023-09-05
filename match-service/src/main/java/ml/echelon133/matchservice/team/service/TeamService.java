package ml.echelon133.matchservice.team.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.team.dto.TeamDto;
import ml.echelon133.matchservice.country.model.Country;
import ml.echelon133.matchservice.country.repository.CountryRepository;
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

    // ONLY USE IT FOR READING DATA
    private final CountryRepository countryRepository;

    @Autowired
    public TeamService(TeamRepository teamRepository, CountryRepository countryRepository) {
        this.teamRepository = teamRepository;
        this.countryRepository = countryRepository;
    }

    /**
     * Returns the information about the team with specified id.
     *
     * @param id id of the team
     * @return a dto representing the team
     * @throws ResourceNotFoundException thrown when the team does not exist in the database
     */
    public TeamDto findById(UUID id) throws ResourceNotFoundException {
        return this.teamRepository
                .findTeamById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Team.class, id));
    }

    /**
     * Updates the team's information.
     *
     * @param id id of the team to update
     * @param teamDto dto containing values to be placed in the database
     * @return a dto representing the updated team
     * @throws ResourceNotFoundException thrown when the team or their country does not exist in the database
     */
    public TeamDto updateTeam(UUID id, UpsertTeamDto teamDto) throws ResourceNotFoundException {
        var teamToUpdate = this.teamRepository
                .findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Team.class, id));

        teamToUpdate.setName(teamDto.getName());

        // this `UUID.fromString` should never fail because the CountryId value is pre-validated
        var countryId = UUID.fromString(teamDto.getCountryId());
        // make sure that the country with specified UUID already exists and is not marked as deleted
        var country = this.countryRepository
                .findById(countryId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Country.class, countryId));
        teamToUpdate.setCountry(country);

        return TeamMapper.entityToDto(this.teamRepository.save(teamToUpdate));
    }

    /**
     * Creates the team's entry in the database.
     *
     * @param teamDto dto representing the information about a team that will be saved in the database
     * @return a dto representing the newly saved team
     * @throws ResourceNotFoundException thrown when the team's country does not exist in the database
     */
    public TeamDto createTeam(UpsertTeamDto teamDto) throws ResourceNotFoundException {
        // this `UUID.fromString` should never fail because the CountryId value is pre-validated
        var countryId = UUID.fromString(teamDto.getCountryId());
        // make sure that the country with specified UUID already exists and is not marked as deleted
        var country = this.countryRepository
                .findById(countryId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Country.class, countryId));

        var team = new Team(teamDto.getName(), country);
        return TeamMapper.entityToDto(this.teamRepository.save(team));
    }

    /**
     * Finds all teams whose names contain the specified phrase.
     *
     * @param phrase phrase which needs to appear in the name of the team
     * @param pageable information about the wanted page
     * @return a page of teams which match the filter
     */
    public Page<TeamDto> findTeamsByName(String phrase, Pageable pageable) {
        return this.teamRepository.findAllByNameContaining(phrase, pageable);
    }

    /**
     * Marks a team with the specified id as deleted.
     *
     * @param id id of the team to be marked as deleted
     * @return how many entities have been affected
     */
    public Integer markTeamAsDeleted(UUID id)  {
        return this.teamRepository.markTeamAsDeleted(id);
    }
}
