package ml.echelon133.matchservice.venue.repository;

import ml.echelon133.common.venue.dto.VenueDto;
import ml.echelon133.matchservice.venue.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {

    @Query(nativeQuery = true)
    Optional<VenueDto> findVenueById(UUID id);
}
