package pl.echelon133.competitionservice.competition.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.echelon133.competitionservice.competition.model.UnassignedMatch;

import java.util.UUID;

public interface UnassignedMatchRepository extends JpaRepository<UnassignedMatch, UUID> {
}
