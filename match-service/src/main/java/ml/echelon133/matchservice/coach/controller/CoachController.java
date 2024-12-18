package ml.echelon133.matchservice.coach.controller;

import jakarta.validation.Valid;
import ml.echelon133.common.exception.RequestBodyContentInvalidException;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.exception.ValidationResultMapper;
import ml.echelon133.matchservice.coach.model.CoachDto;
import ml.echelon133.matchservice.coach.model.UpsertCoachDto;
import ml.echelon133.matchservice.coach.service.CoachService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{coachId}")
    public CoachDto getCoach(@PathVariable UUID coachId) throws ResourceNotFoundException {
        return coachService.findById(coachId);
    }

    @GetMapping
    public Page<CoachDto> getCoachesByName(Pageable pageable, @RequestParam String name) {
        return coachService.findCoachesByName(name, pageable);
    }

    @PutMapping("/{coachId}")
    public CoachDto updateCoach(@PathVariable UUID coachId, @RequestBody @Valid UpsertCoachDto coachDto, BindingResult result)
            throws ResourceNotFoundException, RequestBodyContentInvalidException {

        if (result.hasErrors()) {
            throw new RequestBodyContentInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return coachService.updateCoach(coachId, coachDto);
    }

    @PostMapping
    public CoachDto createCoach(@RequestBody @Valid UpsertCoachDto coachDto, BindingResult result) throws RequestBodyContentInvalidException {

        if (result.hasErrors()) {
            throw new RequestBodyContentInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return coachService.createCoach(coachDto);
    }

    @DeleteMapping("/{coachId}")
    public Map<String, Integer> deleteCoach(@PathVariable UUID coachId) {
        return Map.of("deleted", coachService.markCoachAsDeleted(coachId));
    }
}
