package pl.echelon133.competitionservice.competition.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.echelon133.competitionservice.competition.model.TeamStats;

import java.util.Optional;
import java.util.UUID;

public interface TeamStatsRepository extends JpaRepository<TeamStats, UUID> {

    Optional<TeamStats> findByTeamIdAndGroup_Competition_Id(UUID teamId, UUID competitionId);
}
