package pl.echelon133.competitionservice.competition.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.echelon133.competitionservice.competition.model.PlayerStats;

import java.util.Optional;
import java.util.UUID;

public interface PlayerStatsRepository extends JpaRepository<PlayerStats, UUID> {

    Optional<PlayerStats> findByPlayerIdAndCompetition_Id(UUID playerId, UUID competitionId);
}
