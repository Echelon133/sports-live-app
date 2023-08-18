package ml.echelon133.matchservice.venue.controller;

import ml.echelon133.common.exception.FormInvalidException;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.exception.ValidationResultMapper;
import ml.echelon133.common.venue.dto.VenueDto;
import ml.echelon133.matchservice.venue.model.UpsertVenueDto;
import ml.echelon133.matchservice.venue.service.VenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService venueService;

    @Autowired
    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping("/{id}")
    public VenueDto getVenue(@PathVariable UUID id) throws ResourceNotFoundException {
        return venueService.findById(id);
    }

    @GetMapping
    public Page<VenueDto> getVenuesByName(@RequestParam String name) {
        return null;
    }

    @PutMapping("/{id}")
    public VenueDto updateVenue(@PathVariable UUID id, @RequestBody @Valid UpsertVenueDto venueDto, BindingResult result)
            throws ResourceNotFoundException, FormInvalidException {

        if (result.hasErrors()) {
            throw new FormInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return venueService.updateVenue(id, venueDto);
    }

    @PostMapping
    public VenueDto createVenue(@RequestBody @Valid UpsertVenueDto venueDto, BindingResult result) throws FormInvalidException {

        if (result.hasErrors()) {
            throw new FormInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return venueService.createVenue(venueDto);
    }

    @DeleteMapping("/{id}")
    public Map<String, Integer> deleteVenue(@PathVariable UUID id) {
        return Map.of("deleted", venueService.markVenueAsDeleted(id));
    }
}
