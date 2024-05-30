package pl.echelon133.competitionservice.competition.controller;

import pl.echelon133.competitionservice.competition.model.CompetitionDto;
import ml.echelon133.common.exception.RequestBodyContentInvalidException;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.exception.ValidationResultMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pl.echelon133.competitionservice.competition.exceptions.CompetitionInvalidException;
import pl.echelon133.competitionservice.competition.model.PlayerStatsDto;
import pl.echelon133.competitionservice.competition.model.StandingsDto;
import pl.echelon133.competitionservice.competition.model.UpsertCompetitionDto;
import pl.echelon133.competitionservice.competition.service.CompetitionService;

import javax.validation.Valid;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/competitions")
public class CompetitionController {

    private final CompetitionService competitionService;

    @Autowired
    public CompetitionController(CompetitionService competitionService) {
        this.competitionService = competitionService;
    }

    @GetMapping("/{competitionId}")
    public CompetitionDto getCompetition(@PathVariable UUID competitionId) throws ResourceNotFoundException {
        return competitionService.findById(competitionId);
    }

    @DeleteMapping("/{competitionId}")
    public Map<String, Integer> deleteCompetition(@PathVariable UUID competitionId) {
        return Map.of("deleted", competitionService.markCompetitionAsDeleted(competitionId));
    }

    @GetMapping
    public Page<CompetitionDto> getCompetitionsByName(Pageable pageable, @RequestParam String name) {
        return competitionService.findCompetitionsByName(name, pageable);
    }

    @PostMapping
    public Map<String, UUID> createCompetition(
            @Valid @RequestBody UpsertCompetitionDto competitionDto, BindingResult result
    ) throws RequestBodyContentInvalidException, CompetitionInvalidException {

        if (result.hasErrors()) {
            throw new RequestBodyContentInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return Map.of("id", competitionService.createCompetition(competitionDto));
    }

    @GetMapping("/{competitionId}/standings")
    public StandingsDto getStandings(@PathVariable UUID competitionId) throws ResourceNotFoundException {
        return competitionService.findStandings(competitionId);
    }

    @GetMapping("/{competitionId}/player-stats")
    public Page<PlayerStatsDto> getPlayerStats(@PageableDefault(size = 25) Pageable pageable, @PathVariable UUID competitionId) {
        return competitionService.findPlayerStatsByCompetition(competitionId, pageable);
    }
}
