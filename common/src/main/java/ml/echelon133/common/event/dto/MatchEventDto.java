package ml.echelon133.common.event.dto;

import java.util.UUID;

/**
 * Represents already processed match events presented to the client of the API.
 */
public record MatchEventDto(UUID id, MatchEventDetails event) {
}
