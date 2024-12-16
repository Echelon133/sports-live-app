package ml.echelon133.common.event.dto;

import java.io.Serializable;
import java.util.UUID;

public record SerializedPlayer(UUID teamPlayerId, UUID playerId, String name) implements Serializable {
}
