package ml.echelon133.common.event.dto;

import java.io.Serializable;
import java.util.UUID;

public record SerializedTeam(UUID homeTeamId, UUID awayTeamId) implements Serializable {
}
