package pl.echelon133.competitionservice.competition.exceptions;

import java.util.List;
import java.util.UUID;

public class CompetitionMatchAssignmentException extends Exception {

    public CompetitionMatchAssignmentException(List<UUID> matchIds) {
        super(String.format("matches [%s] could not be assigned", matchIds.toArray()));
    }
}
