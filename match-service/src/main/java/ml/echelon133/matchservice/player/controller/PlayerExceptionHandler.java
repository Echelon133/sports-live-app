package ml.echelon133.matchservice.player.controller;

import ml.echelon133.common.exception.AbstractExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice(assignableTypes = {PlayerController.class})
public class PlayerExceptionHandler extends AbstractExceptionHandler {
}
