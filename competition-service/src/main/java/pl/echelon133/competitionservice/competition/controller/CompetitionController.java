package pl.echelon133.competitionservice.competition.controller;

import jakarta.validation.Valid;
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
import pl.echelon133.competitionservice.competition.model.*;
import pl.echelon133.competitionservice.competition.service.CompetitionService;

import java.util.List;
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

    @GetMapping("/{competitionId}/league/rounds/{roundNumber}")
    public List<CompactMatchDto> getMatchesFromRound(
            @PathVariable UUID competitionId, @PathVariable int roundNumber
    ) throws Exception {
        return competitionService.findMatchesByRound(competitionId, roundNumber);
    }

    @PostMapping("/{competitionId}/league/rounds/{roundNumber}")
    public void assignMatchesToRound(
            @PathVariable UUID competitionId,
            @PathVariable int roundNumber,
            @Valid @RequestBody UpsertRoundDto upsertRoundDto,
            BindingResult result
    ) throws Exception {

        if (result.hasErrors()) {
            throw new RequestBodyContentInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        competitionService.assignMatchesToRound(competitionId, roundNumber, upsertRoundDto.matchIds());
    }

    @GetMapping("/{competitionId}/matches/unassigned")
    public Page<CompactMatchDto> getUnassignedMatches(@PathVariable UUID competitionId, Pageable pageable) {
        return competitionService.findUnassignedMatches(competitionId, pageable);
    }

    @DeleteMapping("/{competitionId}")
    public Map<String, Integer> deleteCompetition(@PathVariable UUID competitionId) {
        return Map.of("deleted", competitionService.markCompetitionAsDeleted(competitionId));
    }

    @GetMapping
    public Page<CompetitionDto> getCompetitionsByName(Pageable pageable, @RequestParam String name) {
        return competitionService.findCompetitionsByName(name, pageable);
    }

    @GetMapping("/pinned")
    public List<CompetitionDto> getPinnedCompetitions() {
        return competitionService.findPinnedCompetitions();
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
