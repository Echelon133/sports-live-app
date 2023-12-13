package ml.echelon133.matchservice.event.controller;

import ml.echelon133.common.event.dto.MatchEventDto;
import ml.echelon133.matchservice.event.service.MatchEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
