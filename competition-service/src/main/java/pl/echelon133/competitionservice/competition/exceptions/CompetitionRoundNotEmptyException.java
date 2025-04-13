package pl.echelon133.competitionservice.competition.exceptions;

public class CompetitionRoundNotEmptyException extends Exception {

    public CompetitionRoundNotEmptyException() {
        super("only an empty round can have matches assigned to it");
    }
}
