package ml.echelon133.matchservice.coach.controller;

import ml.echelon133.common.exception.AbstractExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice(assignableTypes = {CoachController.class})
public class CoachExceptionHandler extends AbstractExceptionHandler {

    @NotNull
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        String errorMessage = String.format("'%s' request parameter is required", ex.getParameterName());
        AbstractExceptionHandler.ErrorMessage error = new AbstractExceptionHandler.ErrorMessage(HttpStatus.BAD_REQUEST, request, errorMessage);
        return new ResponseEntity<>(error, new HttpHeaders(), error.getStatus());
    }
}
