package ml.echelon133.matchservice.player.controller;

import ml.echelon133.common.exception.FormInvalidException;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.exception.ValidationResultMapper;
import ml.echelon133.common.player.dto.PlayerDto;
import ml.echelon133.matchservice.country.model.Country;
import ml.echelon133.matchservice.player.model.UpsertPlayerDto;
import ml.echelon133.matchservice.player.service.PlayerService;
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

    @Autowired
    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/{id}")
    public PlayerDto getPlayer(@PathVariable UUID id) throws ResourceNotFoundException {
        return playerService.findById(id);
    }

    @GetMapping
    public Page<PlayerDto> getPlayersByName(Pageable pageable, @RequestParam String name) {
        return playerService.findPlayersByName(name, pageable);
    }

    @PutMapping("/{id}")
    public PlayerDto updatePlayer(@PathVariable UUID id, @RequestBody @Valid UpsertPlayerDto playerDto, BindingResult result)
            throws FormInvalidException, ResourceNotFoundException {

        if (result.hasErrors()) {
            throw new FormInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        try {
            return playerService.updatePlayer(id, playerDto);
        } catch (ResourceNotFoundException exception) {
            // updatePlayer's ResourceNotFoundException can be caused by either Player or Country.
            // If the player could not be found, just rethrow the exception to give the user 404 Not Found.
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
    public PlayerDto createPlayer(@RequestBody @Valid UpsertPlayerDto playerDto, BindingResult result)
            throws FormInvalidException {

        if (result.hasErrors()) {
            throw new FormInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        try {
            return playerService.createPlayer(playerDto);
        } catch (ResourceNotFoundException exception) {
            // createPlayer's ResourceNotFoundException is always caused by Country.
            // If the country's ID is correct but does not correspond to any non-deleted entity in the database,
            // throw FormInvalidException with the message about not being able to find the country with specified id
            throw new FormInvalidException(
                    Map.of("countryId", List.of(exception.getMessage()))
            );
        }
    }

    @DeleteMapping("/{id}")
    public Map<String, Integer> deletePlayer(@PathVariable UUID id) {
        return Map.of("deleted", playerService.markPlayerAsDeleted(id));
    }
}