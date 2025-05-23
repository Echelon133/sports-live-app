package ml.echelon133.matchservice.venue.repository;

import ml.echelon133.matchservice.venue.model.VenueDto;
import ml.echelon133.matchservice.venue.model.Venue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    @DisplayName("findVenueById native query finds empty when the venue does not exist")
    public void findVenueById_VenueDoesNotExist_IsEmpty() {
        var id = UUID.randomUUID();

        // when
        Optional<VenueDto> venueDto = venueRepository.findVenueById(id);

        // then
        assertTrue(venueDto.isEmpty());
    }

    @Test
    @DisplayName("findVenueById native query finds venue when the venue exists")
    public void findVenueById_VenueExists_IsPresent() {
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
        var saved = venueRepository.saveAndFlush(venueToSave);

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

    @Test
    @DisplayName("markVenueAsDeleted native query only affects not deleted venues")
    public void markVenueAsDeleted_VenueAlreadyMarkedAsDeleted_IsNotTouchedByQuery() {
        var venue0 = new Venue("Allianz Arena", null);
        venue0.setDeleted(true); // make deleted by default
        var saved0 = venueRepository.save(venue0);

        // when
        Integer countDeleted = venueRepository.markVenueAsDeleted(saved0.getId());

        // then
        assertEquals(0, countDeleted);
    }

    @Test
    @DisplayName("findAllByNameContaining native query only finds results which contain the specified phrase")
    public void findAllByNameContaining_MultipleVenues_OnlyFindsMatchingVenues() {
        venueRepository.save(new Venue("Nou Camp Nou", 1));
        venueRepository.save(new Venue("Camp Nou", 2));
        venueRepository.save(new Venue("San Siro", 3));

        // when
        Page<VenueDto> result = venueRepository.findAllByNameContaining("Nou", Pageable.ofSize(10));

        // then
        assertEquals(2, result.getTotalElements());
        assertTrue(result.stream().anyMatch(v -> v.getName().equals("Nou Camp Nou") && v.getCapacity() == 1));
        assertTrue(result.stream().anyMatch(v -> v.getName().equals("Camp Nou") && v.getCapacity() == 2));
    }

    @Test
    @DisplayName("findAllByNameContaining native query is case-insensitive")
    public void findAllByNameContaining_MultipleVenues_SearchIsCaseInsensitive() {
        venueRepository.save(new Venue("Nou Camp Nou", 1));
        venueRepository.save(new Venue("Camp Nou", 2));
        venueRepository.save(new Venue("San Siro", 3));

        // when
        Page<VenueDto> result = venueRepository.findAllByNameContaining("nou", Pageable.ofSize(10));

        // then
        assertEquals(2, result.getTotalElements());
        assertTrue(result.stream().anyMatch(v -> v.getName().equals("Nou Camp Nou") && v.getCapacity() == 1));
        assertTrue(result.stream().anyMatch(v -> v.getName().equals("Camp Nou") && v.getCapacity() == 2));
    }

    @Test
    @DisplayName("findAllByNameContaining native query only finds non-deleted results which contain the specified phrase")
    public void findAllByNameContaining_SomeDeletedVenues_OnlyFindsMatchingNonDeletedVenues() {
        var deletedVenue = new Venue("Nou Camp Nou", 1);
        deletedVenue.setDeleted(true);
        venueRepository.save(deletedVenue);
        venueRepository.save(new Venue("Camp Nou", 2));
        venueRepository.save(new Venue("San Siro", 3));

        // when
        Page<VenueDto> result = venueRepository.findAllByNameContaining("Nou", Pageable.ofSize(10));

        // then
        assertEquals(1, result.getTotalElements());
        assertTrue(result.stream().anyMatch(v -> v.getName().equals("Camp Nou") && v.getCapacity() == 2));
    }

    @Test
    @DisplayName("findAllByNameContaining native query takes page size information from Pageable into account")
    public void findAllByNameContaining_PageableCustomPageSize_UsesPageableInfo() {
        // when
        Page<VenueDto> result = venueRepository.findAllByNameContaining("Nou", Pageable.ofSize(8));

        // then
        assertEquals(8, result.getPageable().getPageSize());
    }

    @Test
    @DisplayName("findAllByNameContaining native query takes page number information from Pageable into account")
    public void findAllByNameContaining_PageableCustomPageNumber_UsesPageableInfo() {
        var pageable = PageRequest.ofSize(6).withPage(5);

        // when
        Page<VenueDto> result = venueRepository.findAllByNameContaining("Nou", pageable);

        // then
        var receivedPageable = result.getPageable();
        assertEquals(6, receivedPageable.getPageSize());
        assertEquals(5, receivedPageable.getPageNumber());
    }
}
