package ml.echelon133.matchservice.country.controller;

import ml.echelon133.common.exception.AbstractExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice(assignableTypes = {CountryController.class})
public class CountryExceptionHandler extends AbstractExceptionHandler {
}
