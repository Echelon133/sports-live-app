package ml.echelon133.matchservice.team.repository;

import ml.echelon133.matchservice.team.model.TeamDto;
import ml.echelon133.matchservice.team.model.TeamFormDetailsDto;
import ml.echelon133.matchservice.team.model.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {
    boolean existsByIdAndDeletedFalse(UUID teamId);

    /**
     * Finds a non-deleted team with the specified id.
     *
     * @param id id of the team
     * @return empty {@link Optional} if the team was not found or is marked as deleted, otherwise contains a {@link TeamDto}
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = "SELECT CAST(t.id as varchar) as id, t.name as name, t.crest_url as crestUrl, " +
                    "CAST(c.id as varchar) as countryId, c.name as countryName, c.country_code as countryCode, c.deleted as countryDeleted, " +
                    "CAST(coa.id as varchar) as coachId, coa.name as coachName, coa.deleted as coachDeleted " +
                    "FROM team t JOIN country c ON t.country_id = c.id JOIN coach coa ON t.coach_id = coa.id " +
                    "WHERE t.deleted = false AND t.id = ?1",
            nativeQuery = true
    )
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
    @Query(
            value = "SELECT CAST(t.id as varchar) as id, t.name as name, t.crest_url as crestUrl, " +
                    "CAST(c.id as varchar) as countryId, c.name as countryName, c.country_code as countryCode, c.deleted as countryDeleted, " +
                    "CAST(coa.id as varchar) as coachId, coa.name as coachName, coa.deleted as coachDeleted " +
                    "FROM team t JOIN country c ON t.country_id = c.id JOIN coach coa ON t.coach_id = coa.id " +
                    "WHERE LOWER(t.name) LIKE '%' || LOWER(:phrase) || '%' AND t.deleted = false",
            countQuery = "SELECT COUNT(*) FROM team WHERE LOWER(name) LIKE '%' || LOWER(:phrase) || '%' AND deleted = false",
            nativeQuery = true
    )
    Page<TeamDto> findAllByNameContaining(String phrase, Pageable pageable);


    /**
     * Finds at most 5 of the most recent finished matches of a particular team in a particular competition.
     * This is useful for evaluating the current form of the team.
     *
     * @param teamId id of the team whose form is getting evaluated
     * @param competitionId id of the competition in which the evaluation takes place
     * @return at most 5 most recently finished matches of a team in a particular competition
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = "SELECT CAST(m.id as varchar) as id, m.result as result, m.start_time_utc as startTimeUTC, " +
                    "   m.home_goals as homeGoals, m.away_goals as awayGoals, " +
                    "CAST(ht.id as varchar) as homeTeamId, ht.name as homeTeamName, " +
                    "   ht.crest_url as homeTeamCrestUrl, ht.deleted as homeTeamDeleted, " +
                    "CAST(at.id as varchar) as awayTeamId, at.name as awayTeamName, " +
                    "   at.crest_url as awayTeamCrestUrl, at.deleted as awayTeamDeleted " +
                    "FROM match m " +
                    "JOIN team ht ON m.home_team_id = ht.id " +
                    "JOIN team at ON m.away_team_id = at.id " +
                    "WHERE m.deleted = false AND m.competition_id = :competitionId " +
                    "AND m.status = 'FINISHED' AND (m.home_team_id = :teamId OR m.away_team_id = :teamId) " +
                    "ORDER BY m.start_time_utc DESC LIMIT 5 ",
            nativeQuery = true
    )
    List<TeamFormDetailsDto> findFormEvaluationMatches(UUID teamId, UUID competitionId);

    /**
     * Finds at most 5 of the most recent finished matches of a particular team no matter the competition.
     * This is useful for evaluating the current form of the team in all competitions.
     *
     * @param teamId id of the team whose form is getting evaluated
     * @return at most 5 most recently finished matches of a team in all competitions
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = "SELECT CAST(m.id as varchar) as id, m.result as result, m.start_time_utc as startTimeUTC, " +
                    "   m.home_goals as homeGoals, m.away_goals as awayGoals, " +
                    "CAST(ht.id as varchar) as homeTeamId, ht.name as homeTeamName, " +
                    "   ht.crest_url as homeTeamCrestUrl, ht.deleted as homeTeamDeleted, " +
                    "CAST(at.id as varchar) as awayTeamId, at.name as awayTeamName, " +
                    "   at.crest_url as awayTeamCrestUrl, at.deleted as awayTeamDeleted " +
                    "FROM match m " +
                    "JOIN team ht ON m.home_team_id = ht.id " +
                    "JOIN team at ON m.away_team_id = at.id " +
                    "WHERE m.deleted = false " +
                    "AND m.status = 'FINISHED' AND (m.home_team_id = :teamId OR m.away_team_id = :teamId) " +
                    "ORDER BY m.start_time_utc DESC LIMIT 5 ",
            nativeQuery = true
    )
    List<TeamFormDetailsDto> findGeneralFormEvaluationMatches(UUID teamId);
}
