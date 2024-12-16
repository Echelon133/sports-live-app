package ml.echelon133.matchservice.team.controller;

import ml.echelon133.common.exception.RequestBodyContentInvalidException;
import ml.echelon133.common.exception.RequestParamsInvalidException;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.exception.ValidationResultMapper;
import ml.echelon133.matchservice.match.model.CompactMatchDto;
import ml.echelon133.matchservice.match.service.MatchService;
import ml.echelon133.matchservice.team.exception.NumberAlreadyTakenException;
import ml.echelon133.matchservice.team.model.*;
import ml.echelon133.matchservice.team.service.TeamPlayerService;
import ml.echelon133.matchservice.team.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;
    private final MatchService matchService;
    private final TeamPlayerService teamPlayerService;

    @Autowired
    public TeamController(
            TeamService teamService,
            TeamPlayerService teamPlayerService,
            MatchService matchService
    ) {
        this.teamService = teamService;
        this.teamPlayerService = teamPlayerService;
        this.matchService = matchService;
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
        }
    }

    @DeleteMapping("/{teamId}/players/{teamPlayerId}")
    public Map<String, Integer> deletePlayerAssignment(@PathVariable UUID teamId, @PathVariable UUID teamPlayerId) {
        return Map.of("deleted", teamPlayerService.markTeamPlayerAsDeleted(teamPlayerId));
    }

    @GetMapping
    public Page<TeamDto> getTeamsByCriteria(
            Pageable pageable,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) List<UUID> teamIds
    ) throws RequestParamsInvalidException {

        // either 'name' and 'teamIds' must be provided
        if (name == null && teamIds == null) {
            throw new RequestParamsInvalidException(Map.of(
                    "name", "not provided",
                    "teamIds", "not provided"
            ));
        // 'name' and 'teamIds' are mutually exclusive and must NOT be provided together
        } else if (name != null && teamIds != null) {
            throw new RequestParamsInvalidException(Map.of(
                    "name", "cannot be provided together with 'teamIds'",
                    "teamIds", "cannot be provided together with 'name'"
            ));
        }

        if (name != null) {
            return teamService.findTeamsByName(name, pageable);
        } else {
            return teamService.findTeamsByIds(teamIds, pageable);
        }
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

    @GetMapping("/{teamId}/form")
    public List<TeamFormDto> getTeamForm(@PathVariable UUID teamId, @RequestParam(required = false) UUID competitionId) {
        if (competitionId == null) {
            return teamService.evaluateGeneralForm(teamId);
        } else {
            return teamService.evaluateForm(teamId, competitionId);
        }
    }

    @GetMapping("/{teamId}/matches")
    public List<CompactMatchDto> getTeamMatches(
            @PathVariable UUID teamId,
            @RequestParam String type,
            Pageable pageable
    ) throws RequestParamsInvalidException {
        if (!(type.equalsIgnoreCase("results") || type.equalsIgnoreCase("fixtures"))) {
            throw new RequestParamsInvalidException(Map.of("type", "should be either 'fixtures' or 'results'"));
        }
        boolean matchFinished = type.equalsIgnoreCase("results");
        return matchService.findMatchesByTeam(teamId, matchFinished, pageable);
    }
}
