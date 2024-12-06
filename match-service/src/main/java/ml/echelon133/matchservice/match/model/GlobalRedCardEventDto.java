package ml.echelon133.matchservice.match.model;

import java.util.UUID;

/**
 * Represents a red card given in a match, broadcast via websocket.
 *
 * @param matchId id of the match in which the red card happened
 * @param side side whose player got the red card
 */
public record GlobalRedCardEventDto(UUID matchId, GlobalMatchEvent.EventSide side) implements GlobalMatchEvent {
}
