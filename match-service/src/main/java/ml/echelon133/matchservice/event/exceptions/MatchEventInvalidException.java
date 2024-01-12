package ml.echelon133.matchservice.event.exceptions;

/**
 * Exception thrown when a potential match event fails to satisfy some invariant (i.e. the player who scored
 * does not play in the match, the player got a third yellow card, etc.)
 */
public class MatchEventInvalidException extends Exception {

    public MatchEventInvalidException(String message) {
        super(message);
    }
}
