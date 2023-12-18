package ml.echelon133.matchservice.event.service;

import ml.echelon133.common.event.dto.MatchEventDetails;
import ml.echelon133.common.event.dto.MatchEventDto;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.match.MatchStatus;
import ml.echelon133.matchservice.event.exceptions.MatchEventInvalidException;
import ml.echelon133.matchservice.event.model.MatchEvent;
import ml.echelon133.matchservice.event.model.dto.InsertMatchEvent;
import ml.echelon133.matchservice.event.repository.MatchEventRepository;
import ml.echelon133.matchservice.match.model.Match;
import ml.echelon133.matchservice.match.service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static ml.echelon133.common.match.MatchStatus.VALID_STATUS_CHANGES;

@Service
@Transactional
public class MatchEventService {

    private final MatchService matchService;
    private final MatchEventRepository matchEventRepository;

    @Autowired
    public MatchEventService(MatchService matchService, MatchEventRepository matchEventRepository) {
        this.matchService = matchService;
        this.matchEventRepository = matchEventRepository;
    }

    private static MatchEventDto convertEntityToDto(MatchEvent event) {
        return MatchEventDto.from(event.getId(), event.getEvent());
    }

    /**
     * Finds all events of the match with the specified id.
     *
     * @param matchId id of the match whose events will be fetched
     * @return a list of match events
     */
    public List<MatchEventDto> findAllByMatchId(UUID matchId) {
        return matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)
                .stream().map(MatchEventService::convertEntityToDto).collect(Collectors.toList());
    }

    /**
     * Processes a match event and saves it.
     *
     * @param matchId id of the match to which this event belongs to
     * @param eventDto dto containing information about the event
     * @throws ResourceNotFoundException thrown when the match with the provided id does not exist or is marked as deleted
     */
    public void processEvent(UUID matchId, InsertMatchEvent eventDto)
            throws ResourceNotFoundException, MatchEventInvalidException {
        var match = matchService.findEntityById(matchId);

        if (eventDto instanceof InsertMatchEvent.StatusDto) {
            processStatusEvent(match, (InsertMatchEvent.StatusDto) eventDto);
        } else if (eventDto instanceof InsertMatchEvent.CommentaryDto) {
            processCommentaryEvent(match, (InsertMatchEvent.CommentaryDto) eventDto);
        } else {
            throw new MatchEventInvalidException("handling of this event is not implemented");
        }
    }

    /**
     * Processes the status event and saves it.
     *
     * Updates the status of the match, unless the target status change is invalid
     * (i.e. it's impossible to finish the match that had never even begun).
     *
     * @param match entity representing the match to which this event is related
     * @param statusDto dto containing information about the event
     */
    private void processStatusEvent(Match match, InsertMatchEvent.StatusDto statusDto) throws MatchEventInvalidException {
        // this `MatchStatus.valueOf` should never fail because the status value is pre-validated
        var targetStatus = MatchStatus.valueOf(statusDto.getTargetStatus());
        var validTargetStatuses = VALID_STATUS_CHANGES.get(match.getStatus());

        if (!validTargetStatuses.contains(targetStatus)) {
            throw new MatchEventInvalidException("current status of the match cannot be changed to the requested target status");
        }

        var eventDetails = new MatchEventDetails.StatusDto(
                statusDto.getMinute(),
                match.getCompetitionId(),
                targetStatus
        );
        match.setStatus(targetStatus);
        matchEventRepository.save(new MatchEvent(match, eventDetails));
    }

    /**
     * Processes the commentary event and saves it.
     *
     * @param match entity representing the match to which this event is related
     * @param commentaryDto dto containing information about the event
     */
    private void processCommentaryEvent(Match match, InsertMatchEvent.CommentaryDto commentaryDto) {
        var eventDetails = new MatchEventDetails.CommentaryDto(
                commentaryDto.getMinute(),
                match.getCompetitionId(),
                commentaryDto.getMessage()
        );
        matchEventRepository.save(new MatchEvent(match, eventDetails));
    }
}
