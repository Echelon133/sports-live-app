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

    /**
     * Finds a non-deleted venue with the specified id.
     *
     * @param id id of the venue
     * @return empty {@link Optional} if the venue was not found or is marked as deleted, otherwise contains a {@link VenueDto}
     */
    @Query(value = "SELECT id, name, capacity FROM venue WHERE deleted = false AND id = ?1",
            nativeQuery = true)
    Optional<VenueDto> findVenueById(UUID id);

    /**
     * Marks the venue with the specified id as deleted.
     *
     * @param id id of the venue to be marked as deleted
     * @return count of how many venues had been marked as deleted by this query
     */
    @Modifying
    @Query(value = "UPDATE venue SET deleted = true WHERE id = :id AND deleted = false", nativeQuery = true)
    Integer markVenueAsDeleted(UUID id);

    /**
     * Finds all venues which contain a certain phrase in their name.
     *
     * @param phrase phrase (case-insensitive) which has to appear in the name
     * @param pageable information about the wanted page
     * @return a page containing all venues whose names contain the phrase
     */
    @Query(value = "SELECT id, name, capacity FROM venue WHERE LOWER(name) LIKE '%' || LOWER(:phrase) || '%' AND deleted = false",
            countQuery = "SELECT COUNT(*) FROM venue WHERE LOWER(name) LIKE '%' || LOWER(:phrase) || '%' AND deleted = false",
            nativeQuery = true)
    Page<VenueDto> findAllByNameContaining(String phrase, Pageable pageable);
}
