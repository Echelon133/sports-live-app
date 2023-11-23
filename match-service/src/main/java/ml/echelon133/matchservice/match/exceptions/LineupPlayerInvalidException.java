package ml.echelon133.matchservice.match.exceptions;

/**
 * Exception thrown when the potential match lineup of a team contains at least one player who does not play
 * for the team.
 */
public class LineupPlayerInvalidException extends Exception {

    private static final String template = "at least one of provided %s players does not play for this team";

    public LineupPlayerInvalidException(boolean startingPlayer) {
        super(String.format(template, startingPlayer ? "starting" : "substitute"));
    }
}
