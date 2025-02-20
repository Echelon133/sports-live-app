package pl.echelon133.competitionservice.competition.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.echelon133.competitionservice.competition.model.CompetitionMatch;

import java.util.Optional;
import java.util.UUID;

public interface CompetitionMatchRepository extends JpaRepository<CompetitionMatch, UUID> {
    Optional<CompetitionMatch> findByMatchId(UUID matchId);
}
