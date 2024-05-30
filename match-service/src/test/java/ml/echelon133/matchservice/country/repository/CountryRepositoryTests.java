package ml.echelon133.matchservice.country.repository;

import ml.echelon133.matchservice.country.model.CountryDto;
import ml.echelon133.matchservice.country.model.Country;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Disable kubernetes during tests
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@DataJpaTest
public class CountryRepositoryTests {

    @Autowired
    private CountryRepository countryRepository;


    @Test
    @DisplayName("findCountryById native query finds empty when the country does not exist")
    public void findCountryById_CountryDoesNotExist_IsEmpty() {
        var id = UUID.randomUUID();

        // when
        Optional<CountryDto> countryDto = countryRepository.findCountryById(id);

        // then
        assertTrue(countryDto.isEmpty());
    }

    @Test
    @DisplayName("findCountryById native query finds country when the country exists")
    public void findCountryById_CountryExists_IsPresent() {
        var saved = countryRepository.save(new Country("Poland", "PL"));

        // when
        Optional<CountryDto> countryDto = countryRepository.findCountryById(saved.getId());

        // then
        assertTrue(countryDto.isPresent());
        var result = countryDto.get();
        assertEquals(saved.getName(), result.getName());
        assertEquals(saved.getCountryCode(), result.getCountryCode());
    }

    @Test
    @DisplayName("findCountryById native query does not fetch countries marked as deleted")
    public void findCountryById_CountryMarkedAsDeleted_IsEmpty() {
        var countryToSave = new Country("Poland", "PL");
        countryToSave.setDeleted(true);
        var saved = countryRepository.saveAndFlush(countryToSave);

        // when
        Optional<CountryDto> countryDto = countryRepository.findCountryById(saved.getId());

        // then
        assertTrue(countryDto.isEmpty());
    }

    @Test
    @DisplayName("markCountryAsDeleted native query only affects the country with specified id")
    public void markCountryAsDeleted_SpecifiedCountryId_OnlyMarksSpecifiedCountry() {
        var country0 = new Country("Poland", "PL");
        var country1 = new Country("Portugal", "PT");
        var country2 = new Country("Germany", "DE");

        var saved0 = countryRepository.save(country0);
        countryRepository.save(country1);
        countryRepository.save(country2);

        // when
        Integer countDeleted = countryRepository.markCountryAsDeleted(saved0.getId());
        // findCountryById filters out `deleted` entities
        Optional<CountryDto> country = countryRepository.findCountryById(saved0.getId());

        // then
        assertEquals(1, countDeleted);
        assertTrue(country.isEmpty());
    }

    @Test
    @DisplayName("markCountryAsDeleted native query only affects not deleted countries")
    public void markCountryAsDeleted_CountryAlreadyMarkedAsDeleted_IsNotTouchedByQuery() {
        var country0 = new Country("Poland", "PL");
        country0.setDeleted(true); // make deleted by default
        var saved0 = countryRepository.save(country0);

        // when
        Integer countDeleted = countryRepository.markCountryAsDeleted(saved0.getId());

        // then
        assertEquals(0, countDeleted);
    }

    @Test
    @DisplayName("findAllByNameContaining native query only finds results which contain the specified phrase")
    public void findAllByNameContaining_MultipleCountries_OnlyFindsMatchingCountries() {
        countryRepository.save(new Country("Poland", "PL"));
        countryRepository.save(new Country("Portugal", "PT"));
        countryRepository.save(new Country("Germany", "DE"));

        // when
        Page<CountryDto> result = countryRepository.findAllByNameContaining("Po", Pageable.ofSize(10));

        // then
        assertEquals(2, result.getTotalElements());
        Map<String, String> contents = result.getContent().stream().collect(Collectors.toMap(CountryDto::getName, CountryDto::getCountryCode));
        assertEquals("PL", contents.get("Poland"));
        assertEquals("PT", contents.get("Portugal"));
    }

    @Test
    @DisplayName("findAllByNameContaining native query is case-insensitive")
    public void findAllByNameContaining_MultipleCountries_SearchIsCaseInsensitive() {
        countryRepository.save(new Country("Poland", "PL"));
        countryRepository.save(new Country("Portugal", "PT"));
        countryRepository.save(new Country("Germany", "DE"));

        // when
        Page<CountryDto> result = countryRepository.findAllByNameContaining("po", Pageable.ofSize(10));

        // then
        assertEquals(2, result.getTotalElements());
        Map<String, String> contents = result.getContent().stream().collect(Collectors.toMap(CountryDto::getName, CountryDto::getCountryCode));
        assertEquals("PL", contents.get("Poland"));
        assertEquals("PT", contents.get("Portugal"));
    }

    @Test
    @DisplayName("findAllByNameContaining native query only finds non-deleted results which contain the specified phrase")
    public void findAllByNameContaining_SomeDeletedCountries_OnlyFindsMatchingNonDeletedCountries() {
        var deletedCountry = new Country("Poland", "PL");
        deletedCountry.setDeleted(true);
        countryRepository.save(deletedCountry);
        countryRepository.save(new Country("Portugal", "PT"));
        countryRepository.save(new Country("Germany", "DE"));

        // when
        Page<CountryDto> result = countryRepository.findAllByNameContaining("po", Pageable.ofSize(10));

        // then
        assertEquals(1, result.getTotalElements());
        Map<String, String> contents = result.getContent().stream().collect(Collectors.toMap(CountryDto::getName, CountryDto::getCountryCode));
        assertEquals("PT", contents.get("Portugal"));
    }

    @Test
    @DisplayName("findAllByNameContaining native query takes page size information from Pageable into account")
    public void findAllByNameContaining_PageableCustomPageSize_UsesPageableInfo() {
        // when
        Page<CountryDto> result = countryRepository.findAllByNameContaining("test", Pageable.ofSize(8));

        // then
        assertEquals(8, result.getPageable().getPageSize());
    }

    @Test
    @DisplayName("findAllByNameContaining native query takes page number information from Pageable into account")
    public void findAllByNameContaining_PageableCustomPageNumber_UsesPageableInfo() {
        var pageable = PageRequest.ofSize(6).withPage(5);

        // when
        Page<CountryDto> result = countryRepository.findAllByNameContaining("test", pageable);

        // then
        var receivedPageable = result.getPageable();
        assertEquals(6, receivedPageable.getPageSize());
        assertEquals(5, receivedPageable.getPageNumber());
    }
}
