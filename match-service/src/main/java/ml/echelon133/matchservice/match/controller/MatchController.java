package ml.echelon133.matchservice.match.controller;

import ml.echelon133.common.exception.RequestBodyContentInvalidException;
import ml.echelon133.common.exception.RequestParamsInvalidException;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.exception.ValidationResultMapper;
import ml.echelon133.common.match.dto.CompactMatchDto;
import ml.echelon133.common.match.dto.MatchDto;
import ml.echelon133.matchservice.match.controller.validators.MatchCriteriaValidator;
import ml.echelon133.matchservice.match.model.UpsertMatchDto;
import ml.echelon133.matchservice.match.service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
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
    public Map<UUID, List<CompactMatchDto>> getMatchesByCriteria(MatchCriteriaRequestParams params, BindingResult result, Pageable pageable) throws RequestParamsInvalidException {
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

        // Use a manually triggered Validator because validation annotations placed on controller's arguments
        // do not work with request params.
        // Custom validators require the controller to be annotated with @Validated - this in turn
        // causes tests which use standalone MockMvc to completely ignore running these custom validators because
        // @Validated is only supported in webAppContextSetup version of MockMvc
        matchCriteriaValidator.validate(params, result);
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError fError : result.getFieldErrors()) {
                errors.put(fError.getField(), fError.getDefaultMessage());
            }
            throw new RequestParamsInvalidException(errors);
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
}
