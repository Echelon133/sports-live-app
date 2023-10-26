package ml.echelon133.matchservice.team.controller;

import ml.echelon133.common.exception.RequestBodyContentInvalidException;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.exception.ValidationResultMapper;
import ml.echelon133.common.team.dto.TeamDto;
import ml.echelon133.common.team.dto.TeamPlayerDto;
import ml.echelon133.matchservice.team.exception.NumberAlreadyTakenException;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.team.model.TeamPlayer;
import ml.echelon133.matchservice.team.model.UpsertTeamDto;
import ml.echelon133.matchservice.team.model.UpsertTeamPlayerDto;
import ml.echelon133.matchservice.team.service.TeamPlayerService;
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
    private final TeamPlayerService teamPlayerService;

    @Autowired
    public TeamController(TeamService teamService, TeamPlayerService teamPlayerService) {
        this.teamService = teamService;
        this.teamPlayerService = teamPlayerService;
    }

    @GetMapping("/{teamId}")
    public TeamDto getTeam(@PathVariable UUID teamId) throws ResourceNotFoundException {
        return teamService.findById(teamId);
    }

    @GetMapping("/{teamId}/players")
    public List<TeamPlayerDto> getTeamPlayers(@PathVariable UUID teamId) {
        return teamPlayerService.findAllPlayersOfTeam(teamId);
    }

    @PostMapping("/{teamId}/players")
    public TeamPlayerDto assignPlayerToTeam(@PathVariable UUID teamId,
                                            @RequestBody @Valid UpsertTeamPlayerDto teamPlayerDto,
                                            BindingResult result) throws RequestBodyContentInvalidException, ResourceNotFoundException {

        if (result.hasErrors()) {
            throw new RequestBodyContentInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        try {
            return teamPlayerService.createTeamPlayer(teamId, teamPlayerDto);
        } catch (NumberAlreadyTakenException exception) {
            throw new RequestBodyContentInvalidException(
                    Map.of("number", List.of(exception.getMessage()))
            );
        } catch (ResourceNotFoundException exception) {
            // createTeamPlayer's ResourceNotFoundException can be caused by Team or Player
            if (exception.getResourceClass().equals(Team.class)) {
                // this is a team-related endpoint, rethrow this exception to let the client see the 404 Not Found status
                throw exception;
            } else {
                // must have been caused by the Player
                // exceptions related to contents of the request body should be caught and rethrown
                // instead of throwing 404
                throw new RequestBodyContentInvalidException(
                        Map.of("playerId", List.of(exception.getMessage()))
                );
            }
        }
    }

    @PutMapping("/{teamId}/players/{teamPlayerId}")
    public TeamPlayerDto updatePlayerOfTeam(@PathVariable UUID teamId,
                                            @PathVariable UUID teamPlayerId,
                                            @RequestBody @Valid UpsertTeamPlayerDto teamPlayerDto,
                                            BindingResult result) throws RequestBodyContentInvalidException, ResourceNotFoundException {

        if (result.hasErrors()) {
            throw new RequestBodyContentInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        try {
            return teamPlayerService.updateTeamPlayer(teamPlayerId, teamId, teamPlayerDto);
        } catch (NumberAlreadyTakenException exception) {
            throw new RequestBodyContentInvalidException(
                    Map.of("number", List.of(exception.getMessage()))
            );
        } catch (ResourceNotFoundException exception) {
            // updateTeamPlayer's ResourceNotFoundException can be caused by TeamPlayer, Team, or Player
            if (exception.getResourceClass().equals(Team.class) || exception.getResourceClass().equals(TeamPlayer.class)) {
                // this is a team-related endpoint, rethrow this exception to let the client see the 404 Not Found status
                throw exception;
            } else {
                // must have been caused by Player
                // exceptions related to contents of the request body should be caught and rethrown
                // instead of throwing 404
                throw new RequestBodyContentInvalidException(
                        Map.of("playerId", List.of(exception.getMessage()))
                );
            }
        }
    }

    @DeleteMapping("/{teamId}/players/{teamPlayerId}")
    public Map<String, Integer> deletePlayerAssignment(@PathVariable UUID teamId, @PathVariable UUID teamPlayerId) {
        return Map.of("deleted", teamPlayerService.markTeamPlayerAsDeleted(teamPlayerId));
    }

    @GetMapping
    public Page<TeamDto> getTeamsByName(Pageable pageable, @RequestParam String name) {
        return teamService.findTeamsByName(name, pageable);
    }

    @PutMapping("/{teamId}")
    public TeamDto updateTeam(@PathVariable UUID teamId, @RequestBody @Valid UpsertTeamDto teamDto, BindingResult result)
            throws RequestBodyContentInvalidException, ResourceNotFoundException {

        if (result.hasErrors()) {
            throw new RequestBodyContentInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return teamService.updateTeam(teamId, teamDto);
    }

    @PostMapping
    public TeamDto createTeam(@RequestBody @Valid UpsertTeamDto teamDto, BindingResult result)
            throws RequestBodyContentInvalidException, ResourceNotFoundException {

        if (result.hasErrors()) {
            throw new RequestBodyContentInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return teamService.createTeam(teamDto);
    }

    @DeleteMapping("/{teamId}")
    public Map<String, Integer> deleteTeam(@PathVariable UUID teamId) {
        return Map.of("deleted", teamService.markTeamAsDeleted(teamId));
    }
}
