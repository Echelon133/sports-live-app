package pl.echelon133.competitionservice.competition.exceptions;

/**
 * Exception thrown when a potential competition fails to satisfy some invariant (i.e. team with given id does not
 * exist, therefore it cannot be placed in a competition).
 */
public class CompetitionInvalidException extends Exception {

    public CompetitionInvalidException(String message) {
        super(message);
    }
}
