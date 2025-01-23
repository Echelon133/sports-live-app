package pl.echelon133.competitionservice.competition.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import pl.echelon133.competitionservice.competition.*;
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
        var testDto = CompetitionDto.from(UUID.randomUUID(), "test1", "test2", "test3", 1);
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
        var expectedDto = CompetitionDto.from(UUID.randomUUID(), "test1", "test2", "test3", 1);
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
    @DisplayName("findPinnedCompetitions calls the repository method")
    public void findPinnedCompetitions_NoArguments_CorrectlyCallsRepository() {
        var expectedDto = CompetitionDto.from(UUID.randomUUID(), "test1", "test2", "test3", 1);

        // given
        given(competitionRepository.findAllPinned()).willReturn(List.of(expectedDto));

        // when
        var result = competitionService.findPinnedCompetitions();

        // then
        assertEquals(1, result.size());
        assertEquals(expectedDto, result.get(0));
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
                .leaguePhase(TestUpsertLeaguePhaseDto.builder().groups(List.of(group)).build())
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
                .leaguePhase(TestUpsertLeaguePhaseDto.builder().groups(List.of(group)).build())
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

    private static class CompetitionMatcher implements ArgumentMatcher<Competition> {
        private final Competition expectedCompetition;

        public CompetitionMatcher(Competition expectedCompetition) {
            this.expectedCompetition = expectedCompetition;
        }

        @Override
        public boolean matches(Competition competition) {
            // cannot use `equals` because our entities' implementation of that method only
            // compares IDs of objects, so if we want to compare pure data we need to compare
            // everything "by hand"
            boolean basicDataMatches =
                    competition.getName().equals(expectedCompetition.getName()) &&
                    competition.getSeason().equals(expectedCompetition.getSeason()) &&
                    competition.getLogoUrl().equals(expectedCompetition.getLogoUrl()) &&
                    competition.isPinned() == expectedCompetition.isPinned();

            if (!basicDataMatches) {
                return false;
            }

            if (competition.getLeaguePhase() == null) {
                if (expectedCompetition.getLeaguePhase() != null) {
                    return false;
                }
            } else {
                int groupSize = competition.getLeaguePhase().getGroups().size();
                int expectedGroupSize = expectedCompetition.getLeaguePhase().getGroups().size();

                if (groupSize != expectedGroupSize) {
                    return false;
                }

                for (var i = 0; i < groupSize; i++) {
                    Group group = competition.getLeaguePhase().getGroups().get(i);
                    Group expectedGroup = expectedCompetition.getLeaguePhase().getGroups().get(i);

                    int teamSize = group.getTeams().size();
                    int expectedTeamSize = expectedGroup.getTeams().size();

                    if (teamSize != expectedTeamSize) {
                        return false;
                    }

                    for (var j = 0; j < teamSize; j++) {
                        TeamStats teamStats = group.getTeams().get(j);
                        TeamStats expectedTeamStats = expectedGroup.getTeams().get(j);
                        boolean groupDataMatches =
                                group.getName().equals(expectedGroup.getName()) &&
                                teamStats.getTeamId().equals(expectedTeamStats.getTeamId()) &&
                                teamStats.getTeamName().equals(expectedTeamStats.getTeamName()) &&
                                teamStats.getCrestUrl().equals(expectedTeamStats.getCrestUrl());

                        if (!groupDataMatches) {
                            return false;
                        }
                    }
                }

                int legendSize = competition.getLeaguePhase().getLegend().size();
                for (var i = 0; i < legendSize; i++) {
                    Legend legend = competition.getLeaguePhase().getLegend().get(i);
                    Legend expectedLegend = expectedCompetition.getLeaguePhase().getLegend().get(i);
                    boolean legendDataMatches =
                            legend.getPositions().equals(expectedLegend.getPositions()) &&
                            legend.getContext().equals(expectedLegend.getContext()) &&
                            legend.getSentiment().equals(expectedLegend.getSentiment());

                    if (!legendDataMatches) {
                        return false;
                    }
                }

                boolean maxRoundsMatches =
                        competition.getLeaguePhase().getMaxRounds() == expectedCompetition.getLeaguePhase().getMaxRounds();

                if (!maxRoundsMatches) {
                    return false;
                }
            }

            if (competition.getKnockoutPhase() == null) {
                if (expectedCompetition.getKnockoutPhase() != null) {
                    return false;
                }
            } else {
                int stageSize = competition.getKnockoutPhase().getStages().size();
                int expectedStageSize = expectedCompetition.getKnockoutPhase().getStages().size();

                if (stageSize != expectedStageSize) {
                    return false;
                }

                for (var i = 0; i < stageSize; i++) {
                    Stage stage = competition.getKnockoutPhase().getStages().get(i);
                    Stage expectedStage = expectedCompetition.getKnockoutPhase().getStages().get(i);

                    int slotSize = stage.getSlots().size();
                    int expectedSlotSize = expectedStage.getSlots().size();

                    if (slotSize != expectedSlotSize) {
                        return false;
                    }

                    for (var j = 0; j < slotSize; j++) {
                        KnockoutSlot slot = stage.getSlots().get(j);
                        KnockoutSlot expectedSlot = expectedStage.getSlots().get(j);

                        var slotType = slot.getType();
                        var expectedSlotType = expectedSlot.getType();

                        if (slotType.equals(KnockoutSlot.SlotType.EMPTY) && expectedSlotType.equals(KnockoutSlot.SlotType.EMPTY)) {
                            // EMPTY slot does not have any data, so these values are equal based on type only
                        } else if (slotType.equals(KnockoutSlot.SlotType.BYE) && expectedSlotType.equals(KnockoutSlot.SlotType.BYE)) {
                            KnockoutSlot.Bye bye = (KnockoutSlot.Bye)slot;
                            KnockoutSlot.Bye expectedBye = (KnockoutSlot.Bye)expectedSlot;
                            if (!bye.getTeamId().equals(expectedBye.getTeamId())) {
                                return false;
                            }
                        } else if (slotType.equals(KnockoutSlot.SlotType.TAKEN) && expectedSlotType.equals(KnockoutSlot.SlotType.TAKEN)) {
                            KnockoutSlot.Taken taken = (KnockoutSlot.Taken)slot;
                            KnockoutSlot.Taken expectedTaken = (KnockoutSlot.Taken)expectedSlot;
                            boolean takenMatches =
                                    taken.getFirstLeg().equals(expectedTaken.getFirstLeg()) &&
                                    taken.getSecondLeg().equals(expectedTaken.getSecondLeg());
                            if (!takenMatches) {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    }
                }
            }

            return true;
        }
    }

    @Test
    @DisplayName("createCompetition correctly constructs a competition object when all team details are fetched successfully and competition is not pinned")
    public void createCompetition_NotPinnedCompetitionAndTeamDetailsAllFetchesSucceed_ConstructsEntityAndSaves() throws CompetitionInvalidException {
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
                .leaguePhase(TestUpsertLeaguePhaseDto.builder().groups(List.of(dtoGroup)).legend(List.of(dtoLegend)).maxRounds(25).build())
                .build();

        var expectedTeamStats = new TeamStats(groupTeamId, "Team " + groupTeamId, "Url " + groupTeamId);
        var expectedGroup = new Group(dtoGroup.name(), List.of(expectedTeamStats));
        var expectedLegend = new Legend(dtoLegend.positions(), dtoLegend.context(), Legend.LegendSentiment.POSITIVE_A);
        var expectedCompetition = new Competition(
                dtoCompetition.name(),
                dtoCompetition.season(),
                dtoCompetition.logoUrl()
        );
        expectedCompetition.setLeaguePhase(new LeaguePhase(List.of(expectedGroup), List.of(expectedLegend), dtoCompetition.leaguePhase().maxRounds()));

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
        given(competitionRepository.save(any(Competition.class))).willReturn(new Competition());

        // when
        var result = competitionService.createCompetition(dtoCompetition);

        // then
        verify(competitionRepository).save(argThat(new CompetitionMatcher(expectedCompetition)));
        assertNotNull(result);
    }

    @Test
    @DisplayName("createCompetition correctly constructs a competition object when all team details are fetched successfully and competition is pinned")
    public void createCompetition_PinnedCompetitionAndTeamDetailsAllFetchesSucceed_ConstructsEntityAndSaves() throws CompetitionInvalidException {
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
                .leaguePhase(TestUpsertLeaguePhaseDto.builder().groups(List.of(dtoGroup)).legend(List.of(dtoLegend)).maxRounds(10).build())
                .pinned(true)
                .build();

        var expectedTeamStats = new TeamStats(groupTeamId, "Team " + groupTeamId, "Url " + groupTeamId);
        var expectedGroup = new Group(dtoGroup.name(), List.of(expectedTeamStats));
        var expectedLegend = new Legend(dtoLegend.positions(), dtoLegend.context(), Legend.LegendSentiment.POSITIVE_A);
        var expectedCompetition = new Competition(
                dtoCompetition.name(),
                dtoCompetition.season(),
                dtoCompetition.logoUrl()
        );
        expectedCompetition.setLeaguePhase(
                new LeaguePhase(List.of(expectedGroup), List.of(expectedLegend), dtoCompetition.leaguePhase().maxRounds())
        );
        expectedCompetition.setPinned(true);

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
        given(competitionRepository.save(any(Competition.class))).willReturn(new Competition());

        // when
        var result = competitionService.createCompetition(dtoCompetition);

        // then
        verify(competitionRepository).save(argThat(new CompetitionMatcher(expectedCompetition)));
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
                .leaguePhase(new LeaguePhase(List.of(group), List.of(legend)))
                .build();
        var competitionId = competition.getId();

        // given
        given(competitionRepository.findById(competitionId)).willReturn(Optional.of(competition));

        // when
        StandingsDto result = competitionService.findStandings(competitionId);

        // then
        var groupsDto = result.groups();
        assertEquals(1, groupsDto.size());

        var groupDto = groupsDto.get(0);
        var teamsDto = groupDto.teams();
        assertEquals(group.getName(), groupDto.name());
        assertEquals(1, teamsDto.size());

        var teamDto = teamsDto.get(0);
        assertEquals(team.getTeamId(), teamDto.teamId());
        assertEquals(team.getTeamName(), teamDto.teamName());
        assertEquals(team.getCrestUrl(), teamDto.crestUrl());
        assertEquals(team.getMatchesPlayed(), teamDto.matchesPlayed());
        assertEquals(team.getWins(), teamDto.wins());
        assertEquals(team.getDraws(), teamDto.draws());
        assertEquals(team.getLosses(), teamDto.losses());
        assertEquals(team.getGoalsScored(), teamDto.goalsScored());
        assertEquals(team.getGoalsConceded(), teamDto.goalsConceded());
        assertEquals(team.getPoints(), teamDto.points());

        var legendsDto = result.legend();
        assertEquals(1, legendsDto.size());

        var legendDto = legendsDto.get(0);
        assertEquals(legend.getPositions(), legendDto.positions());
        assertEquals(legend.getContext(), legendDto.context());
        assertEquals(legend.getSentiment().toString(), legendDto.sentiment());
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
