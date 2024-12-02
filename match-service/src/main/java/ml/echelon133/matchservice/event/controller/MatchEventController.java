package ml.echelon133.matchservice.event.controller;

import ml.echelon133.common.event.dto.MatchEventDto;
import ml.echelon133.common.exception.RequestBodyContentInvalidException;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.exception.ValidationResultMapper;
import ml.echelon133.matchservice.event.exceptions.MatchEventInvalidException;
import ml.echelon133.matchservice.event.model.dto.InsertMatchEvent;
import ml.echelon133.matchservice.event.service.MatchEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
public class MatchEventController {

    private final MatchEventService matchEventService;

    @Autowired
    public MatchEventController(MatchEventService matchEventService) {
        this.matchEventService = matchEventService;
    }

    @GetMapping("/{matchId}/events")
    public List<MatchEventDto> getEvents(@PathVariable UUID matchId) {
        return matchEventService.findAllByMatchId(matchId);
    }
    
    @PostMapping("/{matchId}/events")
    public void processMatchEvent(
            @PathVariable UUID matchId, @Valid @RequestBody InsertMatchEvent eventDto, BindingResult result
    ) throws RequestBodyContentInvalidException, ResourceNotFoundException, MatchEventInvalidException {

        if (result.hasErrors()){
            throw new RequestBodyContentInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        matchEventService.processEvent(matchId, eventDto);
    } 
}
