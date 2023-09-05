package ml.echelon133.matchservice.venue.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.venue.dto.VenueDto;
import ml.echelon133.matchservice.venue.model.UpsertVenueDto;
import ml.echelon133.matchservice.venue.model.Venue;
import ml.echelon133.matchservice.venue.repository.VenueRepository;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class VenueServiceTests {

    @Mock
    private VenueRepository venueRepository;

    @InjectMocks
    private VenueService venueService;

    @Test
    @DisplayName("findById throws when there is no entity in the repository")
    public void findById_EntityNotPresent_Throws() {
        var testId = UUID.randomUUID();

        // given
        given(venueRepository.findVenueById(testId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            venueService.findById(testId);
        }).getMessage();

        // then
        assertEquals(String.format("venue %s could not be found", testId), message);
    }

    @Test
    @DisplayName("findById returns the dto when the venue is present")
    public void findById_EntityPresent_ReturnsDto() throws ResourceNotFoundException {
        var testDto = VenueDto.from(UUID.randomUUID(), "Camp Nou", 99354);
        var testId = testDto.getId();

        // given
        given(venueRepository.findVenueById(testId)).willReturn(Optional.of(testDto));

        // when
        VenueDto dto = venueService.findById(testId);

        // then
        assertEquals(testDto, dto);
    }

    @Test
    @DisplayName("createVenue calls repository's save and returns correct dto")
    public void createVenue_ValidDto_CorrectlySavesAndReturns() {
        var idToSave = UUID.randomUUID();
        var initialDto = new UpsertVenueDto("Alianz Arena", 75024);
        var entity = new Venue(initialDto.getName(), initialDto.getCapacity());
        entity.setId(idToSave);

        // given
        given(venueRepository.save(argThat(v ->
                v.getName().equals(initialDto.getName()) && v.getCapacity().equals(initialDto.getCapacity())
        ))).willReturn(entity);

        // when
        VenueDto savedDto = venueService.createVenue(initialDto);

        // then
        verify(venueRepository, times(1)).save(any());
        assertEquals(entity.getId(), savedDto.getId());
        assertEquals(entity.getName(), savedDto.getName());
        assertEquals(entity.getCapacity(), savedDto.getCapacity());
    }

    @Test
    @DisplayName("markVenueAsDeleted calls repository's method and returns correct number of entries marked as deleted")
    public void markVenueAsDeleted_ProvidedId_CorrectlyCallsMethodAndReturnsCount() {
        var idToDelete = UUID.randomUUID();

        // given
        given(venueRepository.markVenueAsDeleted(idToDelete)).willReturn(1);

        // when
        Integer countDeleted = venueService.markVenueAsDeleted(idToDelete);

        // then
        assertEquals(1, countDeleted);
    }

    @Test
    @DisplayName("updateVenue throws when there is no entity in the repository")
    public void updateVenue_EntityNotPresent_Throws() {
        var testId = UUID.randomUUID();

        // given
        given(venueRepository.findById(testId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            venueService.updateVenue(testId, new UpsertVenueDto("Test", null));
        }).getMessage();

        // then
        assertEquals(String.format("venue %s could not be found", testId), message);
    }

    @Test
    @DisplayName("updateVenue throws when there is no entity in the repository because it's been deleted")
    public void updateVenue_EntityMarkedAsDeleted_Throws() {
        var testId = UUID.randomUUID();
        var entity = new Venue("Test", 1000);
        entity.setDeleted(true);

        // given
        given(venueRepository.findById(testId)).willReturn(Optional.of(entity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            venueService.updateVenue(testId, new UpsertVenueDto("Test", null));
        }).getMessage();

        // then
        assertEquals(String.format("venue %s could not be found", testId), message);
    }


    @Test
    @DisplayName("updateVenue changes the fields of the entity")
    public void updateVenue_EntityPresent_UpdatesFieldValuesOfEntity() throws ResourceNotFoundException {
        var originalEntity = new Venue("Test", 2000);
        var newName = "San Siro";
        var newCapacity = 80018;
        var expectedUpdatedEntity = new Venue(newName, newCapacity);
        expectedUpdatedEntity.setId(originalEntity.getId());

        // given
        given(venueRepository.findById(originalEntity.getId())).willReturn(Optional.of(originalEntity));
        given(venueRepository.save(
                argThat(v -> v.getName().equals(newName) && v.getCapacity() == newCapacity)
        )).willReturn(expectedUpdatedEntity);

        // when
        VenueDto updated = venueService.updateVenue(originalEntity.getId(), new UpsertVenueDto(newName, newCapacity));

        // then
        assertEquals(newName, updated.getName());
        assertEquals(newCapacity, updated.getCapacity());
    }

    @Test
    @DisplayName("findVenuesByName correctly calls the repository method")
    public void findVenuesByName_CustomPhraseAndPageable_CorrectlyCallsRepository() {
        var phrase = "test";
        var pageable = Pageable.ofSize(7).withPage(4);
        var expectedDto = VenueDto.from(UUID.randomUUID(), "test", 1000);
        var expectedPage = new PageImpl<>(List.of(expectedDto), pageable, 1);

        // given
        given(venueRepository.findAllByNameContaining(
                eq(phrase),
                argThat(p -> p.getPageSize() == 7 && p.getPageNumber() == 4)
        )).willReturn(expectedPage);

        // when
        var result = venueService.findVenuesByName(phrase, pageable);

        // then
        assertEquals(1, result.getNumberOfElements());
    }
}
