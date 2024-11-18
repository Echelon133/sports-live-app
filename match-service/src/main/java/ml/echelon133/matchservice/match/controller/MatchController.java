package ml.echelon133.matchservice.match.controller;

import ml.echelon133.common.exception.RequestBodyContentInvalidException;
import ml.echelon133.common.exception.RequestParamsInvalidException;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.exception.ValidationResultMapper;
import ml.echelon133.matchservice.match.controller.validators.MatchCriteriaValidator;
import ml.echelon133.matchservice.match.exceptions.LineupPlayerInvalidException;
import ml.echelon133.matchservice.match.model.*;
import ml.echelon133.matchservice.match.service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;
    private final MatchCriteriaValidator matchCriteriaValidator;

    @Autowired
    public MatchController(MatchService matchService, MatchCriteriaValidator matchCriteriaValidator) {
        this.matchService = matchService;
        this.matchCriteriaValidator = matchCriteriaValidator;
    }

    @GetMapping("/{matchId}")
    public MatchDto getMatch(@PathVariable UUID matchId) throws ResourceNotFoundException {
        return matchService.findById(matchId);
    }

    @PostMapping
    public MatchDto createMatch(@RequestBody @Valid UpsertMatchDto matchDto, BindingResult result) throws RequestBodyContentInvalidException, ResourceNotFoundException {

        if (result.hasErrors()) {
            throw new RequestBodyContentInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return matchService.createMatch(matchDto);
    }

    @PutMapping("/{matchId}")
    public MatchDto updateMatch(@PathVariable UUID matchId, @RequestBody @Valid UpsertMatchDto matchDto, BindingResult result)
            throws RequestBodyContentInvalidException, ResourceNotFoundException {

        if (result.hasErrors()) {
            throw new RequestBodyContentInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return matchService.updateMatch(matchId, matchDto);
    }

    @DeleteMapping("/{matchId}")
    public Map<String, Integer> deleteMatch(@PathVariable UUID matchId) {
        return Map.of("deleted", matchService.markMatchAsDeleted(matchId));
    }

    @GetMapping
    public Map<UUID, List<CompactMatchDto>> getMatchesByCriteria(
            MatchCriteriaRequestParams params,
            BindingResult result,
            Pageable pageable
    ) throws RequestParamsInvalidException {
        // there are two variants of results this endpoint provides:
        //      * variant 1 - matches that happen on a specific date (in a specific timezone)
        //      * variant 2 - matches that happen in a specific competition and have a specific type
        //                      (results - matches that are finished, fixtures - ongoing and future matches)
        //
        //  both variants support paging (size and page parameters)
        //
        // example valid calls which represent the first variant:
        //      * /api/matches?date=2023/01/01
        //      * /api/matches?date=2023/10/10&utcOffset=%2B02:00 (encode the plus sign as '%2B')
        //      * /api/matches?date=2023/10/10&utcOffset=-02:00
        //
        // example valid calls which represent the second variant:
        //      * /api/matches?competitionId=6a2b04c0-b391-435f-bd36-982abcabd4a2&type=fixtures
        //      * /api/matches?competitionId=6a2b04c0-b391-435f-bd36-982abcabd4a2&type=results

        // Validation annotations placed on request parameters are not used unless the entire controller is annotated
        // with '@Validated'. The standalone configuration of MockMvc does not support the '@Validated' annotation
        // which results in all validation annotations of request parameters being ignored.
        // To have a testable controller without having to create a 'webAppContextSetup' version of MockMvc we
        // have to use Spring Validators and trigger them manually.
        matchCriteriaValidator.validate(params, result);
        if (result.hasErrors()) {
            throw new RequestParamsInvalidException(ValidationResultMapper.requestParamResultIntoErrorMap(result));
        }

        if (params.getDate() != null) {
            // handle the first variant (date and utcOffset)
            LocalDate date = LocalDate.parse(params.getDate(), matchCriteriaValidator.getMatchDateFormatter());
            ZoneOffset zoneOffset = ZoneOffset.UTC;
            if (params.getUtcOffset() != null) {
                zoneOffset = ZoneOffset.of(params.getUtcOffset());
            }
            return matchService.findMatchesByDate(date, zoneOffset, pageable);
        } else {
            // handle the second variant (competitionId and type)
            UUID competitionId = UUID.fromString(params.getCompetitionId());
            boolean matchFinished = params.getType().equalsIgnoreCase("results");
            return matchService.findMatchesByCompetition(competitionId, matchFinished, pageable);
        }
    }

    @GetMapping("/{matchId}/lineups")
    public LineupDto getMatchLineup(@PathVariable UUID matchId) {
        return matchService.findMatchLineup(matchId);
    }

    @PutMapping("/{matchId}/lineups/{side:home|away}")
    public void updateLineup(
            @PathVariable UUID matchId,
            @PathVariable String side,
            @RequestBody @Valid UpsertLineupDto lineupDto,
            BindingResult result
    ) throws RequestBodyContentInvalidException, ResourceNotFoundException, LineupPlayerInvalidException {

        if (result.hasErrors()) {
            throw new RequestBodyContentInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        // side's regex guarantees that its value is either 'home' or 'away'
        if (side.equals("home")) {
            matchService.updateHomeLineup(matchId, lineupDto);
        } else {
            matchService.updateAwayLineup(matchId, lineupDto);
        }
    }
}
