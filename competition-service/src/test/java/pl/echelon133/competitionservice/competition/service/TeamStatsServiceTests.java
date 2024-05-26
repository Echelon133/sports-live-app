package pl.echelon133.competitionservice.competition.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.echelon133.competitionservice.competition.model.TeamStats;
import pl.echelon133.competitionservice.competition.repository.TeamStatsRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class TeamStatsServiceTests {

    @Mock
    private TeamStatsRepository teamStatsRepository;

    @InjectMocks
    private TeamStatsService teamStatsService;

    @Test
    @DisplayName("findTeamStats returns stats entity when it's present in the database")
    public void findTeamStats_EntityPresent_ReturnsEntity() throws ResourceNotFoundException {
        var teamId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();

        var teamStats = new TeamStats();

        // given
        given(teamStatsRepository.findByTeamIdAndGroup_Competition_Id(teamId, competitionId))
                .willReturn(Optional.of(teamStats));

        // when
        var stats = teamStatsService.findTeamStats(teamId, competitionId);

        // then
        assertEquals(teamStats, stats);
    }

    @Test
    @DisplayName("findTeamStats throws when entity is not present in the database")
    public void findTeamStats_EntityNotPresent_Throws() throws ResourceNotFoundException {
        var teamId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();

        // given
        given(teamStatsRepository.findByTeamIdAndGroup_Competition_Id(teamId, competitionId))
                .willReturn(Optional.empty());

        // when
        var message = assertThrows(ResourceNotFoundException.class, () -> {
            teamStatsService.findTeamStats(teamId, competitionId);
        }).getMessage();

        // then
        var expectedMessage = String.format("teamstats %s could not be found", teamId);
        assertEquals(expectedMessage, message);
    }
}
