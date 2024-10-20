package ml.echelon133.matchservice.team.repository;

import ml.echelon133.matchservice.team.model.TeamDto;
import ml.echelon133.matchservice.team.model.TeamPlayerDto;
import ml.echelon133.matchservice.team.model.TeamPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TeamPlayerRepository extends JpaRepository<TeamPlayer, UUID> {
    boolean existsByIdAndDeletedFalse(UUID teamPlayerId);

    /**
     * Finds all non-deleted players who are currently playing for the team with specified id.
     *
     * @param teamId id of the team whose players need to be found
     * @return a list of players
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = "SELECT CAST(tp.id as varchar) as id, tp.position as position, tp.number as number, " +
                    "CAST(p.id as varchar) as playerId, p.name as name, p.date_of_birth as dateOfBirth, " +
                    "p.country_code as countryCode " +
                    "FROM team_player tp " +
                    "JOIN team t ON tp.team_id = t.id " +
                    "JOIN player p ON tp.player_id = p.id " +
                    "WHERE tp.deleted = false AND tp.team_id = :teamId AND t.deleted = false AND p.deleted = false",
            nativeQuery = true
    )
    List<TeamPlayerDto> findAllPlayersByTeamId(UUID teamId);

    /**
     * Finds all non-deleted teams for which the player with specified id is currently playing.
     *
     * @param playerId id of the player whose teams need to be found
     * @return a list of teams
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = "SELECT CAST(t.id as varchar) as id, t.name as name, t.crest_url as crestUrl, " +
                    "t.country_code as countryCode, " +
                    "CAST(coa.id as varchar) as coachId, coa.name as coachName, coa.deleted as coachDeleted " +
                    "FROM team t " +
                    "JOIN coach coa ON t.coach_id = coa.id " +
                    "JOIN team_player tp ON tp.team_id = t.id " +
                    "JOIN player p ON tp.player_id = p.id " +
                    "WHERE tp.deleted = false AND t.deleted = false AND tp.player_id = :playerId AND p.deleted = false",
            nativeQuery = true
    )
    List<TeamDto> findAllTeamsOfPlayerByPlayerId(UUID playerId);

    /**
     * Marks the binding between the team and the player as deleted.
     *
     * @param id id of the binding
     * @return count of how many bindings have been marked as deleted by this query
     */
    @Modifying
    @Query(value = "UPDATE team_player SET deleted = true WHERE id = :id AND deleted = false", nativeQuery = true)
    Integer markTeamPlayerAsDeleted(UUID id);

    /**
     * Returns `true` if the team with specified id already has a player with the given number.
     * @param teamId id of the team
     * @param numberToCheck shirt number we want to check
     * @return true if the number is already taken, otherwise false
     */
    @Query(
            value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END " +
                    "FROM team_player tp " +
                    "JOIN team t ON tp.team_id = t.id " +
                    "JOIN player p ON tp.player_id = p.id " +
                    "WHERE tp.team_id = :teamId AND tp.number = :numberToCheck AND p.deleted = false AND t.deleted = false",
            nativeQuery = true
    )
    boolean teamHasPlayerWithNumber(UUID teamId, Integer numberToCheck);
}
