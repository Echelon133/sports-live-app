package ml.echelon133.common.event.dto;

import java.util.UUID;

public record MatchEventDto(UUID id, MatchEventDetails event) {
}
