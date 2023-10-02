package ml.echelon133.matchservice.match.controller;

import ml.echelon133.common.exception.FormInvalidException;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.exception.ValidationResultMapper;
import ml.echelon133.common.match.dto.MatchDto;
import ml.echelon133.common.match.dto.MatchStatusDto;
import ml.echelon133.matchservice.match.model.Match;
import ml.echelon133.matchservice.match.model.UpsertMatchDto;
import ml.echelon133.matchservice.match.service.MatchService;
import ml.echelon133.matchservice.referee.model.Referee;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.venue.model.Venue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    @Autowired
    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/{matchId}")
    public MatchDto getMatch(@PathVariable UUID matchId) throws ResourceNotFoundException {
        return matchService.findById(matchId);
    }

    @GetMapping("/{matchId}/status")
    public MatchStatusDto getMatchStatus(@PathVariable UUID matchId) throws ResourceNotFoundException {
        return matchService.findStatusById(matchId);
    }

    @PostMapping
    public MatchDto createMatch(@RequestBody @Valid UpsertMatchDto matchDto, BindingResult result) throws FormInvalidException {

        if (result.hasErrors()) {
            throw new FormInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        try {
            return matchService.createMatch(matchDto);
        } catch (ResourceNotFoundException ex) {
            // createMatch throws ResourceNotFoundException when:
            //      * either of the teams was not found (homeTeamId or awayTeamId points to a non-existent/deleted team)
            //      * venue was not found (venueId points to a non-existent/deleted venue)
            //      * referee was not found (refereeId points to a non-existent/deleted referee)
            var resourceClass = ex.getResourceClass();
            var fieldName = "";
            if (resourceClass.equals(Team.class)) {
                fieldName = "homeTeamId";
                if (matchDto.getAwayTeamId().equals(ex.getResourceId().toString())) {
                    fieldName = "awayTeamId";
                }
            } else if (resourceClass.equals(Venue.class)) {
                fieldName = "venueId";
            } else  if (resourceClass.equals(Referee.class)) {
                fieldName = "refereeId";
            } else {
                // all expected causes of the exception are handled above, anything other than that is
                // unexpected
                throw new RuntimeException("unexpected resource class " + resourceClass);
            }
            throw new FormInvalidException(Map.of(fieldName, List.of(ex.getMessage())));
        }
    }

    @PutMapping("/{matchId}")
    public MatchDto updateMatch(@PathVariable UUID matchId, @RequestBody @Valid UpsertMatchDto matchDto, BindingResult result)
            throws FormInvalidException, ResourceNotFoundException {

        if (result.hasErrors()) {
            throw new FormInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        try {
            return matchService.updateMatch(matchId, matchDto);
        } catch (ResourceNotFoundException ex) {
            // updateMatch throws ResourceNotFoundException when:
            //      * match was not found
            //      * either of the teams was not found (homeTeamId or awayTeamId points to a non-existent/deleted team)
            //      * venue was not found (venueId points to a non-existent/deleted venue)
            //      * referee was not found (refereeId points to a non-existent/deleted referee)
            var resourceClass = ex.getResourceClass();
            var fieldName = "";
            if (resourceClass.equals(Match.class)) {
                // rethrow to give the client 404
                throw ex;
            } else if (resourceClass.equals(Team.class)) {
                fieldName = "homeTeamId";
                if (matchDto.getAwayTeamId().equals(ex.getResourceId().toString())) {
                    fieldName = "awayTeamId";
                }
            } else if (resourceClass.equals(Venue.class)) {
                fieldName = "venueId";
            } else if (resourceClass.equals(Referee.class)) {
                fieldName = "refereeId";
            } else {
                // all expected causes of the exception are handled above, anything other than that is
                // unexpected
                throw new RuntimeException("unexpected resource class " + resourceClass);
            }
            throw new FormInvalidException(Map.of(fieldName, List.of(ex.getMessage())));
        }
    }

    @DeleteMapping("/{matchId}")
    public Map<String, Integer> deleteMatch(@PathVariable UUID matchId) {
        return Map.of("deleted", matchService.markMatchAsDeleted(matchId));
    }
}
