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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
    @DisplayName("findById maps found entity into a dto with identical values")
    public void findById_EntityPresent_MapsIntoValidDto() throws ResourceNotFoundException {
        var testDto = new VenueDto(UUID.randomUUID(), "Camp Nou", 99354);
        var testId = testDto.getId();

        // given
        given(venueRepository.findVenueById(testId)).willReturn(Optional.of(testDto));

        // when
        VenueDto dto = venueService.findById(testId);

        // then
        assertEquals(testDto.getId(), dto.getId());
        assertEquals(testDto.getName(), dto.getName());
        assertEquals(testDto.getCapacity(), dto.getCapacity());
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
}
