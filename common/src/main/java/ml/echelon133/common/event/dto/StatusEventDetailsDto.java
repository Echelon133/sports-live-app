package ml.echelon133.common.event.dto;

import ml.echelon133.common.event.MatchEventType;
import ml.echelon133.common.match.MatchResult;
import ml.echelon133.common.match.MatchStatus;

import java.util.UUID;

/**
 * Data class representing already processed match event of type <b>STATUS</b>.
 */
public record StatusEventDetailsDto(
        String minute,
        UUID competitionId,
        MatchStatus targetStatus,
        SerializedTeam teams,
        MatchResult result,
        SerializedScore mainScore
) implements MatchEventDetails {

    @Override
    public String type() {
        return MatchEventType.STATUS.name();
    }
}
