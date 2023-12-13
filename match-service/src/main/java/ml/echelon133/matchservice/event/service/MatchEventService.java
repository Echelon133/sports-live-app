package ml.echelon133.matchservice.event.service;

import ml.echelon133.common.event.dto.MatchEventDto;
import ml.echelon133.matchservice.event.model.MatchEvent;
import ml.echelon133.matchservice.event.repository.MatchEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MatchEventService {

    private final MatchEventRepository matchEventRepository;

    @Autowired
    public MatchEventService(MatchEventRepository matchEventRepository) {
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
}
