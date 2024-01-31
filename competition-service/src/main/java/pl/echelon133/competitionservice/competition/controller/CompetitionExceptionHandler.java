package pl.echelon133.competitionservice.competition.controller;

import ml.echelon133.common.exception.AbstractExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice(assignableTypes = {CompetitionController.class})
public class CompetitionExceptionHandler extends AbstractExceptionHandler {
}
