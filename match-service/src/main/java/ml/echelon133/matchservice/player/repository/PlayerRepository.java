package ml.echelon133.matchservice.player.repository;

import ml.echelon133.matchservice.player.model.PlayerDto;
import ml.echelon133.matchservice.player.model.Player;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository extends JpaRepository<Player, UUID> {
    boolean existsByIdAndDeletedFalse(UUID playerId);

    /**
     * Finds a non-deleted player with the specified id.
     *
     * @param id id of the player
     * @return empty {@link Optional} if the player was not found or is marked as deleted, otherwise contains a {@link PlayerDto}
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = "SELECT CAST(p.id as varchar) as id, p.name as name, p.position as position, p.date_of_birth as dateOfBirth, " +
                    "CAST(c.id as varchar) as countryId, c.name as countryName, c.country_code as countryCode, c.deleted as countryDeleted " +
                    "FROM player p JOIN country c ON p.country_id = c.id WHERE p.deleted = false AND p.id = ?1",
            nativeQuery = true
    )
    Optional<PlayerDto> findPlayerById(UUID id);

    /**
     * Marks the player with the specified id as deleted.
     *
     * @param id id of the player to be marked as deleted
     * @return count of how many players had been marked as deleted by this query
     */
    @Modifying
    @Query(value = "UPDATE player SET deleted = true WHERE id = :id AND deleted = false", nativeQuery = true)
    Integer markPlayerAsDeleted(UUID id);

    /**
     * Finds all players whose names contain a certain phrase.
     *
     * @param phrase phrase (case-insensitive) which has to appear in the name
     * @param pageable information about the wanted page
     * @return a page containing all players whose names contain the phrase
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = "SELECT CAST(p.id as varchar) as id, p.name as name, p.position as position, p.date_of_birth as dateOfBirth, " +
                    "CAST(c.id as varchar) as countryId, c.name as countryName, c.country_code as countryCode, c.deleted as countryDeleted " +
                    "FROM player p JOIN country c ON p.country_id = c.id WHERE LOWER(p.name) LIKE '%' || LOWER(:phrase) || '%' AND p.deleted = false",
            countQuery = "SELECT COUNT(*) FROM player WHERE LOWER(name) LIKE '%' || LOWER(:phrase) || '%' AND deleted = false",
            nativeQuery = true
    )
    Page<PlayerDto> findAllByNameContaining(String phrase, Pageable pageable);
}
