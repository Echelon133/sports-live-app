package ml.echelon133.matchservice.match.controller;

import ml.echelon133.common.exception.FormInvalidException;
import ml.echelon133.common.exception.RequestParamsInvalidException;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.exception.ValidationResultMapper;
import ml.echelon133.common.match.dto.CompactMatchDto;
import ml.echelon133.common.match.dto.MatchDto;
import ml.echelon133.common.match.dto.MatchStatusDto;
import ml.echelon133.matchservice.match.model.Match;
import ml.echelon133.matchservice.match.model.UpsertMatchDto;
import ml.echelon133.matchservice.match.service.MatchService;
import ml.echelon133.matchservice.referee.model.Referee;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.venue.model.Venue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;
    private final DateTimeFormatter MATCH_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/d");
    private final String DATE_PARAM_NAME = "date";
    private final String UTC_OFFSET_PARAM_NAME = "utcOffset";
    private final String COMPETITION_ID_PARAM_NAME = "competitionId";
    private final String TYPE_PARAM_NAME = "type";

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

    /**
     * Helper method which handles the first variant of listing matches. This variant requires the client of the API
     * to specify the date and the timezone offset.
     *
     * @param params request parameters provided with the request
     * @param pageable information about the wanted page
     * @return a map where the keys are IDs of competitions and the values are lists of matches that happen on a specific date
     * @throws RequestParamsInvalidException thrown when request parameters provided with the request are not valid
     */
    private Map<UUID, List<CompactMatchDto>> handleMatchesByDate(Map<String, String> params, Pageable pageable) throws RequestParamsInvalidException {
        Map<String, String> paramValidationErrors = new HashMap<>();

        // extract `date` and turn it into LocalDate
        LocalDate date = null;
        try {
            date = LocalDate.parse(params.get(DATE_PARAM_NAME), MATCH_DATE_FORMATTER);
        } catch (DateTimeException ignore) {
            paramValidationErrors.put(DATE_PARAM_NAME, "format should be yyyy/mm/dd");
        }

        // extract `utcOffset` and turn it into ZoneOffset
        // (if `utcOffset` was not provided, assume "Z" which is simply UTC)
        ZoneOffset zoneOffset = ZoneOffset.of("Z");
        if (params.containsKey(UTC_OFFSET_PARAM_NAME)) {
            try {
                zoneOffset = ZoneOffset.of(params.get(UTC_OFFSET_PARAM_NAME));
            } catch (DateTimeException ignore) {
                paramValidationErrors.put(UTC_OFFSET_PARAM_NAME, "format should be ±hh:mm");
            }
        }

        if (paramValidationErrors.isEmpty()) {
            return matchService.findMatchesByDate(date, zoneOffset, pageable);
        }
        throw new RequestParamsInvalidException(paramValidationErrors);
    }

    /**
     * Helper method which handles the second variant of listing matches. This variant requires the client of the API
     * to specify the ID of the competition and the type of results that are expected. Type 'fixtures' makes the query
     * fetch matches that are either ongoing or happening in the future. The other type - 'results' - causes fetching
     * of matches that are finished.
     *
     * @param params request parameters provided with the request
     * @param pageable information about the wanted page
     * @return a map with at most a single key (the ID of the competition), where the value is a list of
     *      matches that belong to the competition and are of specified type
     * @throws RequestParamsInvalidException thrown when request parameters provided with the request are not valid
     */
    private Map<UUID, List<CompactMatchDto>> handleMatchesByCompetition(Map<String, String> params, Pageable pageable) throws RequestParamsInvalidException {
        Map<String, String> paramValidationErrors = new HashMap<>();
        UUID competitionId = null;
        Boolean showFinishedMatches = null;

        try {
            competitionId = UUID.fromString(params.get(COMPETITION_ID_PARAM_NAME));
        } catch (IllegalArgumentException ignore) {
            paramValidationErrors.put(COMPETITION_ID_PARAM_NAME, "not a uuid");
        }

        if (params.containsKey(TYPE_PARAM_NAME)) {
            var typeValue = params.get(TYPE_PARAM_NAME);
            if (typeValue.equalsIgnoreCase("results")) {
                showFinishedMatches = true;
            } else if (typeValue.equalsIgnoreCase("fixtures")) {
                showFinishedMatches = false;
            } else {
                paramValidationErrors.put(TYPE_PARAM_NAME, "should be either 'fixtures' or 'results'");
            }
        } else {
            paramValidationErrors.put(TYPE_PARAM_NAME, "not provided");
        }

        if (paramValidationErrors.isEmpty()) {
            return matchService.findMatchesByCompetition(competitionId, showFinishedMatches, pageable);
        }
        throw new RequestParamsInvalidException(paramValidationErrors);
    }

    @GetMapping
    public Map<UUID, List<CompactMatchDto>> getMatchesByCriteria(@RequestParam Map<String, String> params, Pageable pageable) throws RequestParamsInvalidException {
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

        // make the search of request param names case-insensitive
        var caseInsensitiveParams = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveParams.putAll(params);

        // find out if the combination of all the request params actually represents a variant which is valid
        if (caseInsensitiveParams.keySet().containsAll(Set.of(DATE_PARAM_NAME, COMPETITION_ID_PARAM_NAME))) {
            // invalid call - both `date` and `competitionId` present in the same request
            throw new RequestParamsInvalidException(Map.of(
                    DATE_PARAM_NAME, String.format("cannot be provided together with '%s'", COMPETITION_ID_PARAM_NAME),
                    COMPETITION_ID_PARAM_NAME, String.format("cannot be provided together with '%s'", DATE_PARAM_NAME)
            ));
        } else if (caseInsensitiveParams.containsKey(DATE_PARAM_NAME)) {
            // handle the first variant
            return handleMatchesByDate(caseInsensitiveParams, pageable);
        } else if (caseInsensitiveParams.containsKey(COMPETITION_ID_PARAM_NAME)) {
            // handle the second variant
            return handleMatchesByCompetition(caseInsensitiveParams, pageable);
        } else {
            // invalid call - neither `date` nor `competitionId` present in the request
            throw new RequestParamsInvalidException(Map.of(
                    DATE_PARAM_NAME, "not provided",
                    COMPETITION_ID_PARAM_NAME, "not provided"
            ));
        }
    }
}
