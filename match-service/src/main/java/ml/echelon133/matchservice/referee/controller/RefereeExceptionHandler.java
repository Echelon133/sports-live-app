package ml.echelon133.matchservice.referee.controller;

import ml.echelon133.common.exception.AbstractExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice(assignableTypes = {RefereeController.class})
public class RefereeExceptionHandler extends AbstractExceptionHandler {
}
