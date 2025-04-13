package pl.echelon133.competitionservice.competition.exceptions;

/**
 * Exception thrown when a competition does not have a specified round.
 */
public class CompetitionRoundNotFoundException extends Exception {

    public CompetitionRoundNotFoundException(int round) {
        super(String.format("round %d could not be found", round));
    }
}
