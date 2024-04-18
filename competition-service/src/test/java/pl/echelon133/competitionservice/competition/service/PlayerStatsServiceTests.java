package pl.echelon133.competitionservice.competition.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.echelon133.competitionservice.competition.model.Competition;
import pl.echelon133.competitionservice.competition.model.PlayerStats;
import pl.echelon133.competitionservice.competition.repository.CompetitionRepository;
import pl.echelon133.competitionservice.competition.repository.PlayerStatsRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PlayerStatsServiceTests {

    @Mock
    private PlayerStatsRepository playerStatsRepository;

    @Mock
    private CompetitionRepository competitionRepository;

    @InjectMocks
    private PlayerStatsService playerStatsService;

    private PlayerStats createTestPlayerStats(UUID playerId, UUID competitionId, UUID teamId, String name) {
        var stats = new PlayerStats(playerId, teamId, name);
        var competition = new Competition("Test Competition", "2023/24", "");
        competition.setId(competitionId);
        stats.setCompetition(competition);
        return stats;
    }

    @Test
    @DisplayName("findPlayerStatsOrDefault returns found stats object if it's present in the database")
    public void findPlayerStatsOrDefault_StatsPresent_ReturnsStats() throws ResourceNotFoundException {
        var playerId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();
        var teamId = UUID.randomUUID();
        var name = "Test Player";
        var stats = createTestPlayerStats(playerId, competitionId, teamId, name);

        // given
        given(playerStatsRepository.findByPlayerIdAndCompetition_Id(playerId, competitionId)).willReturn(
                Optional.of(stats)
        );

        // when
        var receivedStats = playerStatsService
                .findPlayerStatsOrDefault(playerId, competitionId, teamId, name);

        // then
        assertEquals(playerId, receivedStats.getPlayerId());
        assertEquals(competitionId, receivedStats.getCompetition().getId());
        assertEquals(teamId, receivedStats.getTeamId());
        assertEquals(name, receivedStats.getName());

        verify(playerStatsRepository, times(0)).save(any());
        verify(competitionRepository, times(0)).findById(any());
    }

    @Test
    @DisplayName("findPlayerStatsOrDefault returns new stats object if it's not present in the database")
    public void findPlayerStatsOrDefault_StatsNotPresent_ReturnsNewStats() throws ResourceNotFoundException {
        var playerId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();
        var teamId = UUID.randomUUID();
        var name = "Test Player";

        var competition = new Competition("Test Competition", "2023/24", "");
        competition.setId(competitionId);

        // given
        given(playerStatsRepository.findByPlayerIdAndCompetition_Id(playerId, competitionId)).willReturn(
                Optional.empty()
        );
        given(competitionRepository.findById(competitionId)).willReturn(Optional.of(competition));

        // when
        var receivedStats = playerStatsService
                .findPlayerStatsOrDefault(playerId, competitionId, teamId, name);

        // then
        assertEquals(playerId, receivedStats.getPlayerId());
        assertEquals(competitionId, receivedStats.getCompetition().getId());
        assertEquals(teamId, receivedStats.getTeamId());
        assertEquals(name, receivedStats.getName());
    }

    @Test
    @DisplayName("findPlayerStatsOrDefault throws if both player stats and competition are not present in the database")
    public void findPlayerStatsOrDefault_StatsAndCompetitionNotPresent_Throws() {
        var playerId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();
        var teamId = UUID.randomUUID();
        var name = "Test Player";

        // given
        given(playerStatsRepository.findByPlayerIdAndCompetition_Id(playerId, competitionId)).willReturn(
                Optional.empty()
        );
        given(competitionRepository.findById(competitionId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            playerStatsService.findPlayerStatsOrDefault(playerId, competitionId, teamId, name);
        }).getMessage();

        // then
        assertEquals(String.format("competition %s could not be found", competitionId), message);
    }
}
