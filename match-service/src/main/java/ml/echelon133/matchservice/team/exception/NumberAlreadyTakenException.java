package ml.echelon133.matchservice.team.exception;

import java.util.UUID;

/**
 * Exception thrown when the team already has a player with a specific number.
 */
public class NumberAlreadyTakenException extends Exception {

    public NumberAlreadyTakenException(UUID teamId, Integer number) {
        super(String.format("team %s already has a player with number %d", teamId, number));
    }
}
