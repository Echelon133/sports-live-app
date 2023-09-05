package ml.echelon133.matchservice.team.controller;

import ml.echelon133.common.exception.AbstractExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice(assignableTypes = {TeamController.class})
public class TeamExceptionHandler extends AbstractExceptionHandler {
}
