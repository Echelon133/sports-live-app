package ml.echelon133.matchservice.venue.repository;

import ml.echelon133.common.venue.dto.VenueDto;
import ml.echelon133.matchservice.venue.model.Venue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Disable kubernetes during tests
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@DataJpaTest
public class VenueRepositoryTests {

    @Autowired
    private VenueRepository venueRepository;

    @Test
    @DisplayName("findVenueById native query finds null when the venue does not exist")
    public void findVenueById_VenueDoesNotExist_IsEmpty() {
        var id = UUID.randomUUID();

        // when
        Optional<VenueDto> venueDto = venueRepository.findVenueById(id);

        // then
        assertTrue(venueDto.isEmpty());
    }

    @Test
    @DisplayName("findVenueById native query finds venue when the venue exists")
    public void findVenueById_VenueDoesNotExist_IsPresent() {
        var saved = venueRepository.save(new Venue("San Siro", 80018));

        // when
        Optional<VenueDto> venueDto = venueRepository.findVenueById(saved.getId());

        // then
        assertTrue(venueDto.isPresent());
        var result = venueDto.get();
        assertEquals(result.getName(), saved.getName());
        assertEquals(result.getCapacity(), saved.getCapacity());
    }

    @Test
    @DisplayName("findVenueById native query does not fetch venues marked as deleted")
    public void findVenueById_VenueMarkedAsDeleted_IsEmpty() {
        var venueToSave = new Venue("San Siro", 80018);
        venueToSave.setDeleted(true);
        var saved = venueRepository.save(venueToSave);

        // when
        Optional<VenueDto> venueDto = venueRepository.findVenueById(saved.getId());

        // then
        assertTrue(venueDto.isEmpty());
    }

    @Test
    @DisplayName("markVenueAsDeleted native query only affects the venue with specified id")
    public void markVenueAsDeleted_SpecifiedVenueId_OnlyMarksSpecifiedVenue() {
        var venue0 = new Venue("Allianz Arena", null);
        var venue1 = new Venue("San Siro", null);
        var venue2 = new Venue("Camp Nou", null);

        var saved0 = venueRepository.save(venue0);
        venueRepository.save(venue1);
        venueRepository.save(venue2);

        // when
        Integer countDeleted = venueRepository.markVenueAsDeleted(saved0.getId());
        // findVenueById filters out `deleted` entities
        Optional<VenueDto> venue = venueRepository.findVenueById(saved0.getId());

        // then
        assertEquals(1, countDeleted);
        assertTrue(venue.isEmpty());
    }
}
