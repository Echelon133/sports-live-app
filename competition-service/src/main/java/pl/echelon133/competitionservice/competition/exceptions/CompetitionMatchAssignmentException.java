package pl.echelon133.competitionservice.competition.exceptions;

import java.util.List;
import java.util.UUID;

/**
 * Exception thrown when a match cannot be assigned to a phase of a competition.
 */
public class CompetitionMatchAssignmentException extends Exception {

    public CompetitionMatchAssignmentException(List<UUID> matchIds) {
        super(String.format("matches [%s] could not be assigned", matchIds.toArray()));
    }

    public CompetitionMatchAssignmentException(String message) {
        super(message);
    }
}
