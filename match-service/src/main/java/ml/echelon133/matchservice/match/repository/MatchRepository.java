package ml.echelon133.matchservice.match.repository;

import ml.echelon133.common.match.dto.MatchDto;
import ml.echelon133.common.match.dto.MatchStatusDto;
import ml.echelon133.matchservice.match.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    /**
     * Finds a non-deleted match with the specified id.
     *
     * @param matchId id of the match
     * @return empty {@link Optional} if the match was not found or is marked as deleted, otherwise contains a {@link MatchDto}
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = "SELECT CAST(m.id as varchar) as id, m.status as status, m.result as result, " +
                    "   m.start_time_utc as startTimeUTC, CAST(m.competition_id as varchar) as competitionId, " +
                    "   m.home_goals as homeGoals, m.away_goals as awayGoals, " +
                    "   m.home_penalties as homePenalties, m.away_penalties as awayPenalties, " +
                    "CAST(ht.id as varchar) as homeTeamId, ht.name as homeTeamName, " +
                    "   ht.crest_url as homeTeamCrestUrl, ht.deleted as homeTeamDeleted, " +
                    "CAST(at.id as varchar) as awayTeamId, at.name as awayTeamName, " +
                    "   at.crest_url as awayTeamCrestUrl, at.deleted as awayTeamDeleted, " +
                    "CAST(v.id as varchar) as venueId, v.name as venueName, " +
                    "   v.capacity as venueCapacity, v.deleted as venueDeleted, " +
                    "CAST(r.id as varchar) as refereeId, r.name as refereeName, r.deleted as refereeDeleted " +
                    "FROM match m " +
                    "JOIN team ht ON m.home_team_id = ht.id " +
                    "JOIN team at ON m.away_team_id = at.id " +
                    "JOIN venue v ON m.venue_id = v.id " +
                    "JOIN referee r ON m.referee_id = r.id " +
                    "WHERE m.id = :matchId AND m.deleted = false",
            nativeQuery = true
    )
    Optional<MatchDto> findMatchById(UUID matchId);


    /**
     * Finds the status of a  non-deleted match with the specified id.
     *
     * @param matchId id of the match
     * @return empty {@link Optional} if the match was not found or is marked as deleted, otherwise contains a {@link MatchStatusDto}
     */
    @Query(
            value = "SELECT m.status as status FROM match m WHERE m.id = :matchId AND m.deleted = false",
            nativeQuery = true
    )
    Optional<MatchStatusDto> findMatchStatusById(UUID matchId);

    /**
     * Marks the match with the specified id as deleted.
     *
     * @param matchId id of the match to be marked as deleted
     * @return count of how many matches have been marked as deleted by this query
     */
    @Modifying
    @Query(value = "UPDATE match SET deleted = true WHERE id = :matchId AND deleted = false", nativeQuery = true)
    Integer markMatchAsDeleted(UUID matchId);
}
