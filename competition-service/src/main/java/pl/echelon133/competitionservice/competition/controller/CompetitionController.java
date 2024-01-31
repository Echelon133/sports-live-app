package pl.echelon133.competitionservice.competition.controller;

import ml.echelon133.common.competition.dto.CompetitionDto;
import ml.echelon133.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pl.echelon133.competitionservice.competition.service.CompetitionService;

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
}
