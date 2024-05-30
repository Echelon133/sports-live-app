package ml.echelon133.matchservice.country.service;

import ml.echelon133.matchservice.country.model.CountryDto;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.matchservice.country.model.Country;
import ml.echelon133.matchservice.country.model.UpsertCountryDto;
import ml.echelon133.matchservice.country.repository.CountryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CountryServiceTests {

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private CountryService countryService;


    @Test
    @DisplayName("findById throws when the repository does not store an entity with the given id")
    public void findById_EntityNotPresent_Throws() {
        var testId = UUID.randomUUID();

        // given
        given(countryRepository.findCountryById(testId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            countryService.findById(testId);
        }).getMessage();

        // then
        assertEquals(String.format("country %s could not be found", testId), message);
    }

    @Test
    @DisplayName("findById returns the dto when the country is present")
    public void findById_EntityPresent_ReturnsDto() throws ResourceNotFoundException {
        var testDto = CountryDto.from(UUID.randomUUID(), "Test", "PL");
        var testId = testDto.getId();

        // given
        given(countryRepository.findCountryById(testId)).willReturn(Optional.of(testDto));

        // when
        CountryDto dto = countryService.findById(testId);

        // then
        assertEquals(testDto, dto);
    }
    
    @Test
    @DisplayName("findEntityById throws when the repository does not store an entity with given id")
    public void findEntityById_EntityNotPresent_Throws() {
        var testId = UUID.randomUUID();

        // given
        given(countryRepository.findById(testId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            countryService.findEntityById(testId);
        }).getMessage();

        // then
        assertEquals(String.format("country %s could not be found", testId), message);
    }

    @Test
    @DisplayName("findEntityById throws when the repository stores an entity with given id but it's deleted")
    public void findEntityById_EntityPresentButDeleted_Throws() {
        var testId = UUID.randomUUID();
        var countryEntity = new Country();
        countryEntity.setDeleted(true);

        // given
        given(countryRepository.findById(testId)).willReturn(Optional.of(countryEntity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            countryService.findEntityById(testId);
        }).getMessage();

        // then
        assertEquals(String.format("country %s could not be found", testId), message);
    }

    @Test
    @DisplayName("findEntityById returns the entity when the repository stores it")
    public void findEntityById_EntityPresent_ReturnsEntity() throws ResourceNotFoundException {
        var testId = UUID.randomUUID();
        var countryEntity = new Country();

        // given
        given(countryRepository.findById(testId)).willReturn(Optional.of(countryEntity));

        // when
        var entity = countryService.findEntityById(testId);

        // then
        assertEquals(countryEntity, entity);
    }

    @Test
    @DisplayName("createCountry calls repository's save and returns correct dto")
    public void createCountry_ValidDto_CorrectlySavesAndReturns() {
        var idToSave = UUID.randomUUID();
        var initialDto = new UpsertCountryDto("Test", "PL");
        var entity = new Country(initialDto.getName(), initialDto.getCountryCode());
        entity.setId(idToSave);

        // given
        given(countryRepository.save(argThat(v -> v.getName().equals(initialDto.getName())))).willReturn(entity);

        // when
        CountryDto savedDto = countryService.createCountry(initialDto);

        // then
        verify(countryRepository, times(1)).save(any());
        assertEquals(entity.getId(), savedDto.getId());
        assertEquals(entity.getName(), savedDto.getName());
    }

    @Test
    @DisplayName("markCountryAsDeleted calls repository's method and returns correct number of entries marked as deleted")
    public void markCountryAsDeleted_ProvidedId_CorrectlyCallsMethodAndReturnsCount() {
        var idToDelete = UUID.randomUUID();

        // given
        given(countryRepository.markCountryAsDeleted(idToDelete)).willReturn(1);

        // when
        Integer countDeleted = countryService.markCountryAsDeleted(idToDelete);

        // then
        assertEquals(1, countDeleted);
    }

    @Test
    @DisplayName("updateCountry throws when the repository does not store an entity with the given id")
    public void updateCountry_EntityNotPresent_Throws() {
        var testId = UUID.randomUUID();

        // given
        given(countryRepository.findById(testId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            countryService.updateCountry(testId, new UpsertCountryDto("Test", "PL"));
        }).getMessage();

        // then
        assertEquals(String.format("country %s could not be found", testId), message);
    }

    @Test
    @DisplayName("updateCountry throws when there is no entity in the repository because it's been deleted")
    public void updateCountry_EntityMarkedAsDeleted_Throws() {
        var testId = UUID.randomUUID();
        var entity = new Country("Test", "PL");
        entity.setDeleted(true);

        // given
        given(countryRepository.findById(testId)).willReturn(Optional.of(entity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            countryService.updateCountry(testId, new UpsertCountryDto("Test", "PL"));
        }).getMessage();

        // then
        assertEquals(String.format("country %s could not be found", testId), message);
    }


    @Test
    @DisplayName("updateCountry changes the fields of the entity")
    public void updateCountry_EntityPresent_UpdatesFieldValuesOfEntity() throws ResourceNotFoundException {
        var originalEntity = new Country("Test", "PL");
        var newName = "Asdf";
        var newCountryCode = "DE";
        var expectedUpdatedEntity = new Country(newName, newCountryCode);
        expectedUpdatedEntity.setId(originalEntity.getId());

        // given
        given(countryRepository.findById(originalEntity.getId())).willReturn(Optional.of(originalEntity));
        given(countryRepository.save(argThat(v ->
                v.getName().equals(newName) && v.getCountryCode().equals(newCountryCode)
        ))).willReturn(expectedUpdatedEntity);

        // when
        CountryDto updated = countryService.updateCountry(originalEntity.getId(), new UpsertCountryDto(newName, newCountryCode));

        // then
        assertEquals(newName, updated.getName());
        assertEquals(newCountryCode, updated.getCountryCode());
    }

    @Test
    @DisplayName("findCountriesByName correctly calls the repository method")
    public void findCountriesByName_CustomPhraseAndPageable_CorrectlyCallsRepository() {
        var phrase = "test";
        var pageable = Pageable.ofSize(7).withPage(4);
        var expectedDto = CountryDto.from(UUID.randomUUID(), "test", "PL");
        var expectedPage = new PageImpl<>(List.of(expectedDto), pageable, 1);

        // given
        given(countryRepository.findAllByNameContaining(
                eq(phrase),
                argThat(p -> p.getPageSize() == 7 && p.getPageNumber() == 4)
        )).willReturn(expectedPage);

        // when
        var result = countryService.findCountriesByName(phrase, pageable);

        // then
        assertEquals(1, result.getNumberOfElements());
    }
}
