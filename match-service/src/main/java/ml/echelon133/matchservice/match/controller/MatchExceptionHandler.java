package ml.echelon133.matchservice.match.controller;

import ml.echelon133.common.exception.AbstractExceptionHandler;
import ml.echelon133.matchservice.match.exceptions.LineupPlayerInvalidException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice(assignableTypes = {MatchController.class})
public class MatchExceptionHandler extends AbstractExceptionHandler {

    @ExceptionHandler(value = LineupPlayerInvalidException.class)
    protected ResponseEntity<ErrorMessage> handleLineupPlayerInvalidException(LineupPlayerInvalidException ex, WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.UNPROCESSABLE_ENTITY, request, ex.getMessage());
        return error.asResponseEntity();
    }
}
