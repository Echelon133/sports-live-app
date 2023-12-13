package ml.echelon133.matchservice.event.service;

import ml.echelon133.common.event.dto.MatchEventDetails;
import ml.echelon133.common.match.MatchStatus;
import ml.echelon133.matchservice.event.model.MatchEvent;
import ml.echelon133.matchservice.event.repository.MatchEventRepository;
import ml.echelon133.matchservice.match.TestMatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class MatchEventServiceTests {

    @Mock
    private MatchEventRepository matchEventRepository;

    @InjectMocks
    private MatchEventService matchEventService;

    private MatchEvent createTestStatusMatchEvent(UUID matchId, String minute, MatchStatus targetStatus) {
        var match = TestMatch.builder().id(matchId).build();
        return new MatchEvent(match, new MatchEventDetails.StatusDto(minute, match.getCompetitionId(), targetStatus));
    }

    @Test
    @DisplayName("findAllByMatchId returns an empty list when there are no events")
    public void findAllByMatchId_NoEvents_ReturnsEmptyList() {
        var matchId = UUID.randomUUID();

        // given
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId)).willReturn(List.of());

        // when
        var events = matchEventService.findAllByMatchId(matchId);

        // then
        assertEquals(0, events.size());
    }

    @Test
    @DisplayName("findAllByMatchId returns expected status event dtos")
    public void findAllByMatchId_HasStatusEvent_ReturnsDto() {
        var matchId = UUID.randomUUID();
        var targetStatus = MatchStatus.FIRST_HALF;
        var statusEvent = createTestStatusMatchEvent(matchId, "1", targetStatus);

        // given
        given(matchEventRepository.findAllByMatch_IdOrderByDateCreatedAsc(matchId))
                .willReturn(List.of(statusEvent));

        // when
        var events = matchEventService.findAllByMatchId(matchId);

        // then
        assertEquals(
                1,
                events.stream().filter(e -> {
                    var innerEvent = (MatchEventDetails.StatusDto)e.getEvent();
                    return e.getId().equals(statusEvent.getId()) &&
                            innerEvent.getMinute().equals(statusEvent.getEvent().getMinute()) &&
                            innerEvent.getTargetStatus().equals(targetStatus);
                }).count()
        );
    }
}
