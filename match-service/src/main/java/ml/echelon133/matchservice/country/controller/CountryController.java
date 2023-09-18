package ml.echelon133.matchservice.country.controller;

import ml.echelon133.common.country.dto.CountryDto;
import ml.echelon133.common.exception.FormInvalidException;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.exception.ValidationResultMapper;
import ml.echelon133.matchservice.country.model.UpsertCountryDto;
import ml.echelon133.matchservice.country.service.CountryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    private final CountryService countryService;

    @Autowired
    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }

    @GetMapping("/{countryId}")
    public CountryDto getCountry(@PathVariable UUID countryId) throws ResourceNotFoundException {
        return countryService.findById(countryId);
    }

    @GetMapping
    public Page<CountryDto> getCountriesByName(Pageable pageable, @RequestParam String name) {
        return countryService.findCountriesByName(name, pageable);
    }

    @PutMapping("/{countryId}")
    public CountryDto updateCountry(@PathVariable UUID countryId, @RequestBody @Valid UpsertCountryDto countryDto, BindingResult result)
            throws ResourceNotFoundException, FormInvalidException {

        if (result.hasErrors()) {
            throw new FormInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return countryService.updateCountry(countryId, countryDto);
    }

    @PostMapping
    public CountryDto createCountry(@RequestBody @Valid UpsertCountryDto countryDto, BindingResult result) throws FormInvalidException {

        if (result.hasErrors()) {
            throw new FormInvalidException(ValidationResultMapper.resultIntoErrorMap(result));
        }

        return countryService.createCountry(countryDto);
    }

    @DeleteMapping("/{countryId}")
    public Map<String, Integer> deleteCountry(@PathVariable UUID countryId) {
        return Map.of("deleted", countryService.markCountryAsDeleted(countryId));
    }
}
