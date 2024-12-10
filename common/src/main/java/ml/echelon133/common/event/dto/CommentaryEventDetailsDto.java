package ml.echelon133.common.event.dto;

import ml.echelon133.common.event.MatchEventType;

import java.util.UUID;

/**
 * Data class representing already processed match event of type <b>COMMENTARY</b>.
 */
public record CommentaryEventDetailsDto(
        String minute,
        UUID competitionId,
        String message
) implements MatchEventDetails {

    @Override
    public String type() {
        return MatchEventType.COMMENTARY.name();
    }
}
