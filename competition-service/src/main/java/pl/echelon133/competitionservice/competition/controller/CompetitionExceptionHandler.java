package pl.echelon133.competitionservice.competition.controller;

import ml.echelon133.common.exception.AbstractExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import pl.echelon133.competitionservice.competition.exceptions.*;

@ControllerAdvice(assignableTypes = {CompetitionController.class})
public class CompetitionExceptionHandler extends AbstractExceptionHandler {

    @ExceptionHandler(value = CompetitionInvalidException.class)
    protected ResponseEntity<ErrorMessage> handleCompetitionInvalidException(CompetitionInvalidException ex, WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.UNPROCESSABLE_ENTITY, request, ex.getMessage());
        return error.asResponseEntity();
    }

    @ExceptionHandler(value = CompetitionPhaseNotFoundException.class)
    protected ResponseEntity<ErrorMessage> handleCompetitionPhaseNotFound(CompetitionPhaseNotFoundException ex, WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.NOT_FOUND, request, ex.getMessage());
        return error.asResponseEntity();
    }

    @ExceptionHandler(value = CompetitionRoundNotFoundException.class)
    protected ResponseEntity<ErrorMessage> handleCompetitionRoundNotFoundException(CompetitionRoundNotFoundException ex, WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.NOT_FOUND, request, ex.getMessage());
        return error.asResponseEntity();
    }

    @ExceptionHandler(value = CompetitionRoundNotEmptyException.class)
    protected ResponseEntity<ErrorMessage> handleCompetitionRoundNotEmptyException(CompetitionRoundNotEmptyException ex, WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.UNPROCESSABLE_ENTITY, request, ex.getMessage());
        return error.asResponseEntity();
    }

    @ExceptionHandler(value = CompetitionMatchAssignmentException.class)
    protected ResponseEntity<ErrorMessage> handleCompetitionMatchAssignmentException(CompetitionMatchAssignmentException ex, WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.UNPROCESSABLE_ENTITY, request, ex.getMessage());
        return error.asResponseEntity();
    }
}
