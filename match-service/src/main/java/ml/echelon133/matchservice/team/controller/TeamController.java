package ml.echelon133.matchservice.team.controller;

import ml.echelon133.common.exception.FormInvalidException;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.exception.ValidationResultMapper;
import ml.echelon133.common.team.dto.TeamDto;
import ml.echelon133.matchservice.country.model.Country;
import ml.echelon133.matchservice.team.model.UpsertTeamDto;
import ml.echelon133.matchservice.team.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    @Autowired
    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/{id}")
    public TeamDto getTeam(@PathVariable UUID id) throws ResourceNotFoundException {
        return teamService.findById(id);
    }

    @GetMapping
    public Page<TeamDto> getTeamsByName(Pageable pageable, @RequestParam String name) {
        return teamService.findTeamsByName(name, pageable);
    }

    @PutMapping("/{id}")
    public TeamDto updateTeam(@PathVariable UUID id, @RequestBody @Valid UpsertTeamDto teamDto, BindingResult result)
            throws FormInvalidException, ResourceNotFoundException {

        if (result.hasErrors()) {
            throw new FormInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        try {
            return teamService.updateTeam(id, teamDto);
        } catch (ResourceNotFoundException exception) {
            // updateTeam's ResourceNotFoundException can be caused by either Team or Country.
            // If the team could not be found, just rethrow the exception to give the user 404 Not Found.
            // If the country's ID is correct but does not correspond to any non-deleted entity in the database,
            // throw FormInvalidException with the message about not being able to find the country with specified id
            if (exception.getResourceClass().equals(Country.class)) {
                throw new FormInvalidException(
                        Map.of("countryId", List.of(exception.getMessage()))
                );
            } else {
                // just rethrow the exception, no need for any special handling
                throw exception;
            }
        }
    }

    @PostMapping
    public TeamDto createTeam(@RequestBody @Valid UpsertTeamDto teamDto, BindingResult result)
            throws FormInvalidException {

        if (result.hasErrors()) {
            throw new FormInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        try {
            return teamService.createTeam(teamDto);
        } catch (ResourceNotFoundException exception) {
            // createTeam's ResourceNotFoundException is always caused by Country.
            // If the country's ID is correct but does not correspond to any non-deleted entity in the database,
            // throw FormInvalidException with the message about not being able to find the country with specified id
            throw new FormInvalidException(
                    Map.of("countryId", List.of(exception.getMessage()))
            );
        }
    }

    @DeleteMapping("/{id}")
    public Map<String, Integer> deleteTeam(@PathVariable UUID id) {
        return Map.of("deleted", teamService.markTeamAsDeleted(id));
    }
}
