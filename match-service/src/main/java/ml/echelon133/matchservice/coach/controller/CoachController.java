package ml.echelon133.matchservice.coach.controller;

import ml.echelon133.common.exception.FormInvalidException;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.exception.ValidationResultMapper;
import ml.echelon133.common.coach.dto.CoachDto;
import ml.echelon133.matchservice.coach.model.UpsertCoachDto;
import ml.echelon133.matchservice.coach.service.CoachService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/coaches")
public class CoachController {

    private final CoachService coachService;

    @Autowired
    public CoachController(CoachService coachService) {
        this.coachService = coachService;
    }

    @GetMapping("/{id}")
    public CoachDto getCoach(@PathVariable UUID id) throws ResourceNotFoundException {
        return coachService.findById(id);
    }

    @GetMapping
    public Page<CoachDto> getCoachesByName(Pageable pageable, @RequestParam String name) {
        return coachService.findCoachesByName(name, pageable);
    }

    @PutMapping("/{id}")
    public CoachDto updateCoach(@PathVariable UUID id, @RequestBody @Valid UpsertCoachDto coachDto, BindingResult result)
            throws ResourceNotFoundException, FormInvalidException {

        if (result.hasErrors()) {
            throw new FormInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return coachService.updateCoach(id, coachDto);
    }

    @PostMapping
    public CoachDto createCoach(@RequestBody @Valid UpsertCoachDto coachDto, BindingResult result) throws FormInvalidException {

        if (result.hasErrors()) {
            throw new FormInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return coachService.createCoach(coachDto);
    }

    @DeleteMapping("/{id}")
    public Map<String, Integer> deleteCoach(@PathVariable UUID id) {
        return Map.of("deleted", coachService.markCoachAsDeleted(id));
    }
}
