package pl.echelon133.competitionservice.competition.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.echelon133.competitionservice.competition.model.LeagueSlot;

import java.util.List;
import java.util.UUID;

public interface LeagueSlotRepository extends JpaRepository<LeagueSlot, UUID> {
    List<LeagueSlot> findAllByCompetitionIdAndRoundAndDeletedFalse(UUID competitionId, int round);
}
