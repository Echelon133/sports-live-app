package ml.echelon133.common.event.dto;

import ml.echelon133.common.event.MatchEventType;

import java.util.UUID;

/**
 * Data class representing already processed match event of type <b>SUBSTITUTION</b>.
 */
public record SubstitutionEventDetailsDto(
        String minute,
        UUID competitionId,
        UUID teamId,
        SerializedPlayer playerIn,
        SerializedPlayer playerOut
) implements MatchEventDetails {

    @Override
    public String type() {
        return MatchEventType.SUBSTITUTION.name();
    }
}
