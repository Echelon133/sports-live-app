package ml.echelon133.matchservice.venue.controller;

import ml.echelon133.common.exception.RequestBodyContentInvalidException;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.exception.ValidationResultMapper;
import ml.echelon133.matchservice.venue.model.VenueDto;
import ml.echelon133.matchservice.venue.model.UpsertVenueDto;
import ml.echelon133.matchservice.venue.service.VenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @GetMapping("/{venueId}")
    public VenueDto getVenue(@PathVariable UUID venueId) throws ResourceNotFoundException {
        return venueService.findById(venueId);
    }

    @GetMapping
    public Page<VenueDto> getVenuesByName(Pageable pageable, @RequestParam String name) {
        return venueService.findVenuesByName(name, pageable);
    }

    @PutMapping("/{venueId}")
    public VenueDto updateVenue(@PathVariable UUID venueId, @RequestBody @Valid UpsertVenueDto venueDto, BindingResult result)
            throws ResourceNotFoundException, RequestBodyContentInvalidException {

        if (result.hasErrors()) {
            throw new RequestBodyContentInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return venueService.updateVenue(venueId, venueDto);
    }

    @PostMapping
    public VenueDto createVenue(@RequestBody @Valid UpsertVenueDto venueDto, BindingResult result) throws RequestBodyContentInvalidException {

        if (result.hasErrors()) {
            throw new RequestBodyContentInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return venueService.createVenue(venueDto);
    }

    @DeleteMapping("/{venueId}")
    public Map<String, Integer> deleteVenue(@PathVariable UUID venueId) {
        return Map.of("deleted", venueService.markVenueAsDeleted(venueId));
    }
}
