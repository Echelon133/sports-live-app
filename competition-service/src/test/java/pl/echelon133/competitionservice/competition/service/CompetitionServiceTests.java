package pl.echelon133.competitionservice.competition.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import pl.echelon133.competitionservice.competition.TestCompetition;
import pl.echelon133.competitionservice.competition.TestUpsertCompetitionDto;
import pl.echelon133.competitionservice.competition.TestUpsertGroupDto;
import pl.echelon133.competitionservice.competition.TestUpsertLegendDto;
import pl.echelon133.competitionservice.competition.client.MatchServiceClient;
import pl.echelon133.competitionservice.competition.exceptions.CompetitionInvalidException;
import pl.echelon133.competitionservice.competition.model.*;
import pl.echelon133.competitionservice.competition.repository.CompetitionRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CompetitionServiceTests {

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private MatchServiceClient matchServiceClient;

    @InjectMocks
    private CompetitionService competitionService;

    @Test
    @DisplayName("findById throws when the repository does not store an entity with the given id")
    public void findById_EntityNotPresent_Throws() {
        var competitionId= UUID.randomUUID();

        // given
        given(competitionRepository.findCompetitionById(competitionId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            competitionService.findById(competitionId);
        }).getMessage();

        // then
        assertEquals(String.format("competition %s could not be found", competitionId), message);
    }

    @Test
    @DisplayName("findById returns the dto when the competition is present")
    public void findById_EntityPresent_ReturnsDto() throws ResourceNotFoundException {
        var testDto = CompetitionDto.from(UUID.randomUUID(), "test1", "test2", "test3");
        var competitionId = testDto.getId();

        // given
        given(competitionRepository.findCompetitionById(competitionId)).willReturn(Optional.of(testDto));

        // when
        var dto = competitionService.findById(competitionId);

        // then
        assertEquals(testDto, dto);
    }

    @Test
    @DisplayName("markCompetitionAsDeleted calls repository's method and returns correct number of entries marked as deleted")
    public void markCompetitionAsDeleted_ProvidedId_CorrectlyCallsMethodAndReturnsCount() {
        var idToDelete = UUID.randomUUID();

        // given
        given(competitionRepository.markCompetitionAsDeleted(idToDelete)).willReturn(1);

        // when
        Integer countDeleted = competitionService.markCompetitionAsDeleted(idToDelete);

        // then
        assertEquals(1, countDeleted);
    }

    @Test
    @DisplayName("findCompetitionsByName correctly calls the repository method")
    public void findCompetitionsByName_CustomPhraseAndPageable_CorrectlyCallsRepository() {
        var phrase = "test";
        var pageable = Pageable.ofSize(7).withPage(4);
        var expectedDto = CompetitionDto.from(UUID.randomUUID(), "test1", "test2", "test3");
        var expectedPage = new PageImpl<>(List.of(expectedDto), pageable, 1);

        // given
        given(competitionRepository.findAllByNameContaining(
                eq(phrase),
                argThat(p -> p.getPageSize() == 7 && p.getPageNumber() == 4)
        )).willReturn(expectedPage);

        // when
        var result = competitionService.findCompetitionsByName(phrase, pageable);

        // then
        assertEquals(1, result.getNumberOfElements());
    }

    @Test
    @DisplayName("createCompetition throws when http client returns more teams than requested")
    public void createCompetition_TeamDetailsFetchSizeGreaterThanExpected_ThrowsRuntimeException() {
        var teamId = UUID.randomUUID();

        // request one team
        var requestedTeamIds = List.of(teamId);
        var group = TestUpsertGroupDto.builder()
                .teams(requestedTeamIds.stream().map(UUID::toString).collect(Collectors.toList()))
                .build();
        var dto = TestUpsertCompetitionDto.builder()
                .groups(List.of(group))
                .build();

        // simulate a response with more teams than requested
        var incorrectClientResponse = new PageImpl<>(List.of(
                new TeamDetailsDto(teamId, "Test 1", ""),
                new TeamDetailsDto(UUID.randomUUID(), "Test 2", "")
        ));

        // given
        given(matchServiceClient.getTeamByTeamIds(
                argThat(l -> l.containsAll(requestedTeamIds) && l.size() == requestedTeamIds.size()),
                eq(Pageable.ofSize(requestedTeamIds.size()))
        )).willReturn(incorrectClientResponse);

        // when
        String message = assertThrows(RuntimeException.class, () -> {
            competitionService.createCompetition(dto);
        }).getMessage();

        // then
        assertEquals("validation of teams' data could not be completed successfully", message);
    }

    @Test
    @DisplayName("createCompetition throws when http client returns fewer teams than requested")
    public void createCompetition_TeamDetailsFetchSizeSmallerThanExpected_ThrowsCompetitionInvalidException() {
        var teamId = UUID.randomUUID();
        // id of the team whose details won't be returned by the client
        var missingTeamId = UUID.randomUUID();

        // request two teams
        var requestedTeamIds = List.of(teamId, missingTeamId);
        var group = TestUpsertGroupDto.builder()
                .teams(requestedTeamIds.stream().map(UUID::toString).collect(Collectors.toList()))
                .build();
        var dto = TestUpsertCompetitionDto.builder()
                .groups(List.of(group))
                .build();

        // simulate a response with fewer teams than requested
        var incorrectClientResponse = new PageImpl<>(List.of(
                new TeamDetailsDto(teamId, "Team 1", "")
        ));

        // given
        given(matchServiceClient.getTeamByTeamIds(
                argThat(l -> l.containsAll(requestedTeamIds) && l.size() == requestedTeamIds.size()),
                eq(Pageable.ofSize(requestedTeamIds.size()))
        )).willReturn(incorrectClientResponse);

        // when
        String message = assertThrows(CompetitionInvalidException.class, () -> {
            competitionService.createCompetition(dto);
        }).getMessage();

        // then
        var expectedMessage = String.format("teams with ids [%s] cannot be placed in this competition", missingTeamId);
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("createCompetition correctly constructs a competition object when all team details are fetched successfully")
    public void createCompetition_TeamDetailsAllFetchesSucceed_ConstructsEntityAndSaves() throws CompetitionInvalidException {
        var groupTeamId = UUID.randomUUID();

        var dtoGroup = TestUpsertGroupDto.builder()
                .name("A")
                .teams(List.of(groupTeamId.toString()))
                .build();
        var dtoLegend = TestUpsertLegendDto.builder()
                .positions(Set.of(1))
                .context("Promotion to Competition A")
                .sentiment("POSITIVE_A")
                .build();
        var dtoCompetition = TestUpsertCompetitionDto.builder()
                .name("Test Competition B")
                .season("2024/25")
                .logoUrl("https://test-site.com/image/logo.png")
                .groups(List.of(dtoGroup))
                .legend(List.of(dtoLegend))
                .build();

        var expectedTeamStats = new TeamStats(groupTeamId, "Team " + groupTeamId, "Url " + groupTeamId);
        var expectedGroup = new Group(dtoGroup.getName(), List.of(expectedTeamStats));
        var expectedLegend = new Legend(dtoLegend.getPositions(), dtoLegend.getContext(), Legend.LegendSentiment.POSITIVE_A);
        var expectedCompetition = new Competition(
                dtoCompetition.getName(),
                dtoCompetition.getSeason(),
                dtoCompetition.getLogoUrl(),
                List.of(expectedGroup),
                List.of(expectedLegend)
        );

        var requestedTeamIds = List.of(groupTeamId);
        var clientResponse = new PageImpl<>(List.of(
           new TeamDetailsDto(groupTeamId, expectedTeamStats.getTeamName(), expectedTeamStats.getCrestUrl())
        ));

        // given
        given(matchServiceClient.getTeamByTeamIds(
                argThat(l -> l.containsAll(requestedTeamIds) && l.size() == requestedTeamIds.size()),
                eq(Pageable.ofSize(requestedTeamIds.size()))
        )).willReturn(clientResponse);
        // this simply prevents a NullPointerException, actual checking of the saved value
        // happens in the `verify` at the bottom of this test
        given(competitionRepository.save(any(Competition.class))).willReturn(expectedCompetition);

        // when
        var result = competitionService.createCompetition(dtoCompetition);

        // then
        verify(competitionRepository).save(argThat(competition -> {
            // cannot use `equals` because our entities' implementation of that method only
            // compares IDs of objects, so if we want to compare pure data we need to compare
            // everything "by hand"
            boolean basicDataCorrect =
                    competition.getName().equals(expectedCompetition.getName()) &&
                    competition.getSeason().equals(expectedCompetition.getSeason()) &&
                    competition.getLogoUrl().equals(expectedCompetition.getLogoUrl());

            Group group = competition.getGroups().get(0);
            TeamStats teamStats = group.getTeams().get(0);
            boolean groupDataCorrect =
                    group.getName().equals(expectedGroup.getName()) &&
                    teamStats.getTeamId().equals(expectedTeamStats.getTeamId()) &&
                    teamStats.getTeamName().equals(expectedTeamStats.getTeamName()) &&
                    teamStats.getCrestUrl().equals(expectedTeamStats.getCrestUrl());

            Legend legend = competition.getLegend().get(0);
            boolean legendDataCorrect =
                    legend.getPositions().equals(expectedLegend.getPositions()) &&
                    legend.getContext().equals(expectedLegend.getContext()) &&
                    legend.getSentiment().equals(expectedLegend.getSentiment());

            return basicDataCorrect && groupDataCorrect && legendDataCorrect;
        }));
        assertNotNull(result);
    }

    @Test
    @DisplayName("findStandings throws when entity not found")
    public void findStandings_CompetitionNotFound_Throws() {
        var competitionId = UUID.randomUUID();

        // given
        given(competitionRepository.findById(competitionId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            competitionService.findStandings(competitionId);
        }).getMessage();

        // then
        assertEquals(String.format("competition %s could not be found", competitionId), message);
    }

    @Test
    @DisplayName("findStandings returns expected dto when competition is found")
    public void findStandings_CompetitionFound_ReturnsDto() throws ResourceNotFoundException {
        var team = new TeamStats(UUID.randomUUID(), "Test Team", "http://some-test-url.com/crest.png");
        team.setMatchesPlayed(10);
        team.setWins(7);
        team.setDraws(2);
        team.setLosses(1);
        team.setGoalsScored(30);
        team.setGoalsConceded(14);
        team.setPoints(23);

        var group = new Group("Group A", List.of(team));
        var legend = new Legend(Set.of(1, 2), "Promotion to League A", Legend.LegendSentiment.POSITIVE_A);
        var competition = TestCompetition.builder()
                .groups(List.of(group))
                .legend(List.of(legend))
                .build();
        var competitionId = competition.getId();

        // given
        given(competitionRepository.findById(competitionId)).willReturn(Optional.of(competition));

        // when
        StandingsDto result = competitionService.findStandings(competitionId);

        // then
        var groupsDto = result.getGroups();
        assertEquals(1, groupsDto.size());

        var groupDto = groupsDto.get(0);
        var teamsDto = groupDto.getTeams();
        assertEquals(group.getName(), groupDto.getName());
        assertEquals(1, teamsDto.size());

        var teamDto = teamsDto.get(0);
        assertEquals(team.getTeamId(), teamDto.getTeamId());
        assertEquals(team.getTeamName(), teamDto.getTeamName());
        assertEquals(team.getCrestUrl(), teamDto.getCrestUrl());
        assertEquals(team.getMatchesPlayed(), teamDto.getMatchesPlayed());
        assertEquals(team.getWins(), teamDto.getWins());
        assertEquals(team.getDraws(), teamDto.getDraws());
        assertEquals(team.getLosses(), teamDto.getLosses());
        assertEquals(team.getGoalsScored(), teamDto.getGoalsScored());
        assertEquals(team.getGoalsConceded(), teamDto.getGoalsConceded());
        assertEquals(team.getPoints(), teamDto.getPoints());

        var legendsDto = result.getLegend();
        assertEquals(1, legendsDto.size());

        var legendDto = legendsDto.get(0);
        assertEquals(legend.getPositions(), legendDto.getPositions());
        assertEquals(legend.getContext(), legendDto.getContext());
        assertEquals(legend.getSentiment().toString(), legendDto.getSentiment());
    }

    @Test
    @DisplayName("findPlayerStatsByCompetition correctly calls the repository method")
    public void findPlayerStatsByCompetition_CompetitionIdAndPageable_CorrectlyCallsRepository() {
        var competitionId = UUID.randomUUID();
        var pageable = Pageable.ofSize(7).withPage(4);
        var expectedDto = PlayerStatsDto.from(UUID.randomUUID(), UUID.randomUUID(), "Player");
        var expectedPage = new PageImpl<>(List.of(expectedDto), pageable, 1);

        // given
        given(competitionRepository.findPlayerStats(
                eq(competitionId),
                argThat(p -> p.getPageSize() == 7 && p.getPageNumber() == 4)
        )).willReturn(expectedPage);

        // when
        var result = competitionService.findPlayerStatsByCompetition(competitionId, pageable);

        // then
        assertEquals(1, result.getNumberOfElements());
    }
}
