package ml.echelon133.matchservice.country.service;

import ml.echelon133.common.country.dto.CountryDto;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.matchservice.country.model.Country;
import ml.echelon133.matchservice.country.model.UpsertCountryDto;
import ml.echelon133.matchservice.country.repository.CountryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.UUID;

@Service
@Transactional
public class CountryService {

    private CountryRepository countryRepository;

    @Autowired
    public CountryService(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    private static CountryDto entityToDto(Country country) {
        return CountryDto.from(country.getId(), country.getName(), country.getCountryCode());
    }

    /**
     * Returns the information about the country with specified id.
     *
     * @param id id of the country
     * @return a dto representing the country
     * @throws ResourceNotFoundException thrown when the country does not exist in the database
     */
    public CountryDto findById(UUID id) throws ResourceNotFoundException {
        return this.countryRepository
                .findCountryById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Country.class, id));
    }

    /**
     * Updates the country's information.
     *
     * @param id id of the country to update
     * @param countryDto dto containing values to be placed in the database
     * @return a dto representing the updated country
     * @throws ResourceNotFoundException thrown when the country does not exist in the database
     */
    public CountryDto updateCountry(UUID id, UpsertCountryDto countryDto) throws ResourceNotFoundException {
        var countryToUpdate = this.countryRepository
                .findById(id)
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Country.class, id));

        countryToUpdate.setName(countryDto.getName());
        countryToUpdate.setCountryCode(countryDto.getCountryCode());

        return entityToDto(this.countryRepository.save(countryToUpdate));
    }

    /**
     * Creates the country's entry in the database.
     *
     * @param countryDto dto representing the information about a country that will be saved in the database
     * @return a dto representing the newly saved country
     */
    public CountryDto createCountry(UpsertCountryDto countryDto) {
        return entityToDto(
                this.countryRepository
                        .save(new Country(countryDto.getName(), countryDto.getCountryCode()))
        );
    }

    /**
     * Finds all countries whose names contain the specified phrase.
     *
     * @param phrase phrase which needs to appear in the name of the country
     * @param pageable information about the wanted page
     * @return a page of countries which match the filter
     */
    public Page<CountryDto> findCountriesByName(String phrase, Pageable pageable) {
        return this.countryRepository.findAllByNameContaining(phrase, pageable);
    }

    /**
     * Marks a country with the specified id as deleted.
     *
     * @param id id of the country to be marked as deleted
     * @return how many entities have been affected
     */
    public Integer markCountryAsDeleted(UUID id)  {
        return this.countryRepository.markCountryAsDeleted(id);
    }
}
