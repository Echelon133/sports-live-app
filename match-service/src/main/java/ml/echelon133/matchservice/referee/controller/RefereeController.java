package ml.echelon133.matchservice.referee.controller;

import ml.echelon133.common.exception.FormInvalidException;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.exception.ValidationResultMapper;
import ml.echelon133.common.referee.dto.RefereeDto;
import ml.echelon133.matchservice.referee.service.RefereeService;
import ml.echelon133.matchservice.referee.model.UpsertRefereeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/referees")
public class RefereeController {

    private final RefereeService refereeService;

    @Autowired
    public RefereeController(RefereeService refereeService) {
        this.refereeService = refereeService;
    }

    @GetMapping("/{refereeId}")
    public RefereeDto getReferee(@PathVariable UUID refereeId) throws ResourceNotFoundException {
        return refereeService.findById(refereeId);
    }

    @GetMapping
    public Page<RefereeDto> getRefereesByName(Pageable pageable, @RequestParam String name) {
        return refereeService.findRefereesByName(name, pageable);
    }

    @PutMapping("/{refereeId}")
    public RefereeDto updateReferee(@PathVariable UUID refereeId, @RequestBody @Valid UpsertRefereeDto refereeDto, BindingResult result)
            throws ResourceNotFoundException, FormInvalidException {

        if (result.hasErrors()) {
            throw new FormInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return refereeService.updateReferee(refereeId, refereeDto);
    }

    @PostMapping
    public RefereeDto createReferee(@RequestBody @Valid UpsertRefereeDto refereeDto, BindingResult result) throws FormInvalidException {

        if (result.hasErrors()) {
            throw new FormInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return refereeService.createReferee(refereeDto);
    }

    @DeleteMapping("/{refereeId}")
    public Map<String, Integer> deleteReferee(@PathVariable UUID refereeId) {
        return Map.of("deleted", refereeService.markRefereeAsDeleted(refereeId));
    }
}
