package ml.echelon133.matchservice.player.controller;

import ml.echelon133.common.exception.RequestBodyContentInvalidException;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.exception.ValidationResultMapper;
import ml.echelon133.common.player.dto.PlayerDto;
import ml.echelon133.common.team.dto.TeamDto;
import ml.echelon133.matchservice.country.model.Country;
import ml.echelon133.matchservice.player.model.UpsertPlayerDto;
import ml.echelon133.matchservice.player.service.PlayerService;
import ml.echelon133.matchservice.team.service.TeamPlayerService;
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
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;

    private final TeamPlayerService teamPlayerService;

    @Autowired
    public PlayerController(PlayerService playerService, TeamPlayerService teamPlayerService) {
        this.playerService = playerService;
        this.teamPlayerService = teamPlayerService;
    }

    @GetMapping("/{playerId}")
    public PlayerDto getPlayer(@PathVariable UUID playerId) throws ResourceNotFoundException {
        return playerService.findById(playerId);
    }

    @GetMapping("/{playerId}/teams")
    public List<TeamDto> getTeamsOfPlayer(@PathVariable UUID playerId) throws ResourceNotFoundException {
        return teamPlayerService.findAllTeamsOfPlayer(playerId);
    }

    @GetMapping
    public Page<PlayerDto> getPlayersByName(Pageable pageable, @RequestParam String name) {
        return playerService.findPlayersByName(name, pageable);
    }

    @PutMapping("/{playerId}")
    public PlayerDto updatePlayer(@PathVariable UUID playerId, @RequestBody @Valid UpsertPlayerDto playerDto, BindingResult result)
            throws RequestBodyContentInvalidException, ResourceNotFoundException {

        if (result.hasErrors()) {
            throw new RequestBodyContentInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        try {
            return playerService.updatePlayer(playerId, playerDto);
        } catch (ResourceNotFoundException exception) {
            // updatePlayer's ResourceNotFoundException can be caused by either Player or Country.
            // If the player could not be found, just rethrow the exception to give the user 404 Not Found.
            // If the country's ID is correct but does not correspond to any non-deleted entity in the database,
            // throw FormInvalidException with the message about not being able to find the country with specified id
            if (exception.getResourceClass().equals(Country.class)) {
                throw new RequestBodyContentInvalidException(
                        Map.of("countryId", List.of(exception.getMessage()))
                );
            } else {
                // just rethrow the exception, no need for any special handling
                throw exception;
            }
        }
    }

    @PostMapping
    public PlayerDto createPlayer(@RequestBody @Valid UpsertPlayerDto playerDto, BindingResult result)
            throws RequestBodyContentInvalidException {

        if (result.hasErrors()) {
            throw new RequestBodyContentInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        try {
            return playerService.createPlayer(playerDto);
        } catch (ResourceNotFoundException exception) {
            // createPlayer's ResourceNotFoundException is always caused by Country.
            // If the country's ID is correct but does not correspond to any non-deleted entity in the database,
            // throw FormInvalidException with the message about not being able to find the country with specified id
            throw new RequestBodyContentInvalidException(
                    Map.of("countryId", List.of(exception.getMessage()))
            );
        }
    }

    @DeleteMapping("/{playerId}")
    public Map<String, Integer> deletePlayer(@PathVariable UUID playerId) {
        return Map.of("deleted", playerService.markPlayerAsDeleted(playerId));
    }
}
