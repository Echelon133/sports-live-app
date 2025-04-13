package pl.echelon133.competitionservice.competition.exceptions;

/**
 * Exception thrown when a competition does not have a phase (league or knockout) which is
 * required to complete a certain action (i.e. the client wants to fetch matches from the 2nd round of
 * the league, but that competition does not have a league phase, only a knockout phase).
 */
public class CompetitionPhaseNotFoundException extends Exception {

    public CompetitionPhaseNotFoundException() {
        super("competition does not have the phase required to execute this action");
    }
}
