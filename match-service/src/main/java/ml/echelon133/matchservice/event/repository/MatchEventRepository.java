package ml.echelon133.matchservice.event.repository;

import ml.echelon133.matchservice.event.model.MatchEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatchEventRepository extends JpaRepository<MatchEvent, UUID> {
    List<MatchEvent> findAllByMatch_IdOrderByDateCreatedAsc(UUID matchId);
}
