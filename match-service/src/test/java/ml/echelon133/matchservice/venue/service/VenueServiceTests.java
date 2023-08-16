package ml.echelon133.matchservice.venue.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.venue.dto.VenueDto;
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
import static org.mockito.BDDMockito.given;

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
        given(venueRepository.findById(testId)).willReturn(Optional.empty());

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
        var testEntity = new Venue("Camp Nou", 99354);
        var testId = testEntity.getId();

        // given
        given(venueRepository.findById(testId)).willReturn(Optional.of(testEntity));

        // when
        VenueDto dto = venueService.findById(testId);

        // then
        assertEquals(testEntity.getId(), dto.getId());
        assertEquals(testEntity.getName(), dto.getName());
        assertEquals(testEntity.getCapacity(), dto.getCapacity());
    }
}
