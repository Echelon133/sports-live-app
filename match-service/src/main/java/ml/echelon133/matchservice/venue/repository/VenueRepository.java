package ml.echelon133.matchservice.venue.repository;

import ml.echelon133.matchservice.venue.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {
}
