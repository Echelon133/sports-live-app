package ml.echelon133.matchservice.coach.controller;

import ml.echelon133.common.exception.AbstractExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice(assignableTypes = {CoachController.class})
public class CoachExceptionHandler extends AbstractExceptionHandler {
}
