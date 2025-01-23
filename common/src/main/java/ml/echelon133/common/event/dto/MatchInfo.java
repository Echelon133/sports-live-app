package ml.echelon133.common.event.dto;

import java.util.UUID;

/**
 * Represents information about a new match being created.
 */
public record MatchInfo(UUID competitionId, UUID matchId) {
}
