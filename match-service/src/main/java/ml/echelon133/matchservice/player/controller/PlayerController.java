package ml.echelon133.matchservice.player.controller;

import ml.echelon133.common.exception.FormInvalidException;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.exception.ValidationResultMapper;
import ml.echelon133.common.player.dto.PlayerDto;
import ml.echelon133.matchservice.player.service.PlayerService;
import ml.echelon133.matchservice.player.model.UpsertPlayerDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
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
            throws ResourceNotFoundException, FormInvalidException {

        if (result.hasErrors()) {
            throw new FormInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return playerService.updatePlayer(id, playerDto);
    }

    @PostMapping
    public PlayerDto createPlayer(@RequestBody @Valid UpsertPlayerDto playerDto, BindingResult result) throws FormInvalidException {

        if (result.hasErrors()) {
            throw new FormInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return playerService.createPlayer(playerDto);
    }

    @DeleteMapping("/{id}")
    public Map<String, Integer> deletePlayer(@PathVariable UUID id) {
        return Map.of("deleted", playerService.markPlayerAsDeleted(id));
    }
}
