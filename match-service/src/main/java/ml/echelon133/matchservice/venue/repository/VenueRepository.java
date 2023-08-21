package ml.echelon133.matchservice.venue.repository;

import ml.echelon133.common.venue.dto.VenueDto;
import ml.echelon133.matchservice.venue.model.Venue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {

    @Query(value = "SELECT id, name, capacity FROM venue WHERE deleted = false AND id = ?1",
            nativeQuery = true)
    Optional<VenueDto> findVenueById(UUID id);

    @Modifying
    @Query(value = "UPDATE venue SET deleted = true WHERE id = :id AND deleted = false", nativeQuery = true)
    Integer markVenueAsDeleted(UUID id);

    @Query(value = "SELECT id, name, capacity FROM venue WHERE name LIKE '%' || :phrase || '%' AND deleted = false",
            countQuery = "SELECT COUNT(*) FROM venue WHERE name LIKE '%' || :phrase || '%' AND deleted = false",
            nativeQuery = true)
    Page<VenueDto> findAllByNameContaining(String phrase, Pageable pageable);
}
