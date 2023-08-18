package ml.echelon133.matchservice.venue.repository;

import ml.echelon133.common.venue.dto.VenueDto;
import ml.echelon133.matchservice.venue.model.Venue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
        venueRepository.save(new Venue("Nou Camp Nou", null));
        venueRepository.save(new Venue("Camp Nou", null));
        venueRepository.save(new Venue("San Siro", null));

        // when
        Page<VenueDto> result = venueRepository.findAllByNameContaining("Nou", Pageable.ofSize(10));

        // then
        assertEquals(2, result.getTotalElements());
        List<String> names = result.getContent().stream().map(VenueDto::getName).collect(Collectors.toList());
        assertTrue(names.contains("Nou Camp Nou"));
        assertTrue(names.contains("Camp Nou"));
    }

    @Test
    @DisplayName("findAllByNameContaining native query takes page size information from Pageable into account")
    public void findAllByNameContaining_PageableCustomPageSize_UsesPageableInfo() {
        // when
        Page<VenueDto> result = venueRepository.findAllByNameContaining("Nou", Pageable.ofSize(8));

        // then
        assertEquals(8, result.getPageable().getPageSize());
    }

    // TODO: needs fixing, because some Pageable settings (e.g. setting the page number) cause the native query to fail with
    //  "InvalidDataAccessApiUsage Named query exists but its result type is not compatible"
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
