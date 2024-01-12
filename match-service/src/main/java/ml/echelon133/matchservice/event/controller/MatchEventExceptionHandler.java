package ml.echelon133.matchservice.event.controller;

import ml.echelon133.common.exception.AbstractExceptionHandler;
import ml.echelon133.matchservice.event.exceptions.MatchEventInvalidException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice(assignableTypes = {MatchEventController.class})
public class MatchEventExceptionHandler extends AbstractExceptionHandler {

    @ExceptionHandler(value = MatchEventInvalidException.class)
    protected ResponseEntity<ErrorMessage> handleMatchEventInvalidException(MatchEventInvalidException ex, WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.UNPROCESSABLE_ENTITY, request, ex.getMessage());
        return error.asResponseEntity();
    }
}
