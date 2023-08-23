package ml.echelon133.matchservice.referee.controller;

import ml.echelon133.common.exception.AbstractExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice(assignableTypes = {RefereeController.class})
public class RefereeExceptionHandler extends AbstractExceptionHandler {

    @NotNull
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        String errorMessage = String.format("'%s' request parameter is required", ex.getParameterName());
        ErrorMessage error = new ErrorMessage(HttpStatus.BAD_REQUEST, request, errorMessage);
        return new ResponseEntity<>(error, new HttpHeaders(), error.getStatus());
    }
}
