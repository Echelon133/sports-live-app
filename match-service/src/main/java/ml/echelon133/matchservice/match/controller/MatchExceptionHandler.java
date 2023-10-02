package ml.echelon133.matchservice.match.controller;

import ml.echelon133.common.exception.AbstractExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice(assignableTypes = {MatchController.class})
public class MatchExceptionHandler extends AbstractExceptionHandler {
}
