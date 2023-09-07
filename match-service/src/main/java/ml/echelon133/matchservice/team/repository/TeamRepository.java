package ml.echelon133.matchservice.team.repository;

import ml.echelon133.common.team.dto.TeamDto;
import ml.echelon133.matchservice.team.model.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    /**
     * Finds a non-deleted team with the specified id.
     *
     * @param id id of the team
     * @return empty {@link Optional} if the team was not found or is marked as deleted, otherwise contains a {@link TeamDto}
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(value = "SELECT CAST(t.id as varchar) as id, t.name as name, " +
            "CAST(c.id as varchar) as countryId, c.name as countryName, c.country_code as countryCode, c.deleted as countryDeleted, " +
            "CAST(coa.id as varchar) as coachId, coa.name as coachName, coa.deleted as coachDeleted " +
            "FROM team t JOIN country c ON t.country_id = c.id JOIN coach coa ON t.coach_id = coa.id " +
            "WHERE t.deleted = false AND t.id = ?1",
            nativeQuery = true)
    Optional<TeamDto> findTeamById(UUID id);

    /**
     * Marks the team with the specified id as deleted.
     *
     * @param id id of the team to be marked as deleted
     * @return count of how many teams had been marked as deleted by this query
     */
    @Modifying
    @Query(value = "UPDATE team SET deleted = true WHERE id = :id AND deleted = false", nativeQuery = true)
    Integer markTeamAsDeleted(UUID id);

    /**
     * Finds all teams whose names contain a certain phrase.
     *
     * @param phrase phrase (case-insensitive) which has to appear in the name
     * @param pageable information about the wanted page
     * @return a page containing all teams whose names contain the phrase
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(value = "SELECT CAST(t.id as varchar) as id, t.name as name, " +
            "CAST(c.id as varchar) as countryId, c.name as countryName, c.country_code as countryCode, c.deleted as countryDeleted, " +
            "CAST(coa.id as varchar) as coachId, coa.name as coachName, coa.deleted as coachDeleted " +
            "FROM team t JOIN country c ON t.country_id = c.id JOIN coach coa ON t.coach_id = coa.id " +
            "WHERE LOWER(t.name) LIKE '%' || LOWER(:phrase) || '%' AND t.deleted = false",
            countQuery = "SELECT COUNT(*) FROM team WHERE LOWER(name) LIKE '%' || LOWER(:phrase) || '%' AND deleted = false",
            nativeQuery = true)
    Page<TeamDto> findAllByNameContaining(String phrase, Pageable pageable);
}
