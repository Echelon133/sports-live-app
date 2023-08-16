package ml.echelon133.matchservice.venue.controller;

import ml.echelon133.common.exception.AbstractExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice(assignableTypes = {VenueController.class})
public class VenueExceptionHandler extends AbstractExceptionHandler {
}
