package pl.echelon133.competitionservice.competition.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import pl.echelon133.competitionservice.competition.*;
import pl.echelon133.competitionservice.competition.client.MatchServiceClient;
import pl.echelon133.competitionservice.competition.exceptions.*;
import pl.echelon133.competitionservice.competition.model.*;
import pl.echelon133.competitionservice.competition.repository.CompetitionRepository;
import pl.echelon133.competitionservice.competition.repository.LeagueSlotRepository;
import pl.echelon133.competitionservice.competition.repository.UnassignedMatchRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompetitionServiceTests {

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private UnassignedMatchRepository unassignedMatchRepository;

    @Mock
    private MatchServiceClient matchServiceClient;

    @Mock
    private LeagueSlotRepository leagueSlotRepository;

    @Spy
    private Executor executor = Executors.newFixedThreadPool(2);

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
    @DisplayName("findEntityById throws when the repository does not store an entity with given id")
    public void findEntityById_EntityNotPresent_Throws() {
        var testId = UUID.randomUUID();

        // given
        given(competitionRepository.findById(testId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            competitionService.findEntityById(testId);
        }).getMessage();

        // then
        assertEquals(String.format("competition %s could not be found", testId), message);
    }

    @Test
    @DisplayName("findEntityById throws when the repository stores an entity with given id but it's deleted")
    public void findEntityById_EntityPresentButDeleted_Throws() {
        var testId = UUID.randomUUID();
        var competitionEntity = TestCompetition.builder().deleted(true).build();

        // given
        given(competitionRepository.findById(testId)).willReturn(Optional.of(competitionEntity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            competitionService.findEntityById(testId);
        }).getMessage();

        // then
        assertEquals(String.format("competition %s could not be found", testId), message);
    }

    @Test
    @DisplayName("findEntityById returns the entity when the repository stores it")
    public void findEntityById_EntityPresent_ReturnsEntity() throws ResourceNotFoundException {
        var testId = UUID.randomUUID();
        var competitionEntity = TestCompetition.builder().build();

        // given
        given(competitionRepository.findById(testId)).willReturn(Optional.of(competitionEntity));

        // when
        var entity = competitionService.findEntityById(testId);

        // then
        assertEquals(competitionEntity, entity);
    }


    @Test
    @DisplayName("findUnassignedMatches correctly calls the external endpoint and reuses the pageable")
    public void findUnassignedMatches_MultipleUnassignedMatchesFound_ReturnsPage() {
        var expectedPageSize = 20;
        var expectedPageNumber = 1;
        var competitionId = UUID.randomUUID();
        var pageable = Pageable.ofSize(expectedPageSize).withPage(expectedPageNumber);
        var unassignedMatches = List.of(
                new UnassignedMatch(UUID.randomUUID(), UUID.randomUUID()),
                new UnassignedMatch(UUID.randomUUID(), UUID.randomUUID())
        );
        var expectedMatchIds = unassignedMatches.stream().map(m -> m.getId().getMatchId()).toList();

        // given
        given(
                unassignedMatchRepository.findAllById_CompetitionIdAndAssignedFalse(competitionId, pageable)
        ).willReturn(new PageImpl<>(unassignedMatches));
        given(matchServiceClient.getMatchesById(argThat(l -> l.containsAll(expectedMatchIds)))).willReturn(List.of());

        // when
        var result = competitionService.findUnassignedMatches(competitionId, pageable);

        // then
        assertEquals(0, result.getTotalElements());
        assertEquals(expectedPageSize, result.getPageable().getPageSize());
        assertEquals(expectedPageNumber, result.getPageable().getPageNumber());
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
    @DisplayName("findPinnedCompetitions calls the repository method")
    public void findPinnedCompetitions_NoArguments_CorrectlyCallsRepository() {
        var expectedDto = CompetitionDto.from(UUID.randomUUID(), "test1", "test2", "test3");

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
    @DisplayName("createCompetition correctly constructs a league competition when all team details are fetched successfully and competition is not pinned")
    public void createCompetition_NotPinnedLeagueCompetitionAndTeamDetailsAllFetchesSucceed_ConstructsEntityAndSaves() throws CompetitionInvalidException {
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
    @DisplayName("createCompetition correctly constructs a league competition when all team details are fetched successfully and competition is pinned")
    public void createCompetition_PinnedLeagueCompetitionAndTeamDetailsAllFetchesSucceed_ConstructsEntityAndSaves() throws CompetitionInvalidException {
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

    private List<KnockoutSlot> generateEmptySlots(int number) {
        return IntStream.range(0, number).mapToObj(i -> new KnockoutSlot.Empty()).collect(Collectors.toList());
    }

    @Test
    @DisplayName("createCompetition correctly constructs a knockout competition when it starts at the round of 128")
    public void createCompetition_StartsAtRoundOf128_ConstructsEntityAndSaves() throws CompetitionInvalidException {
        var startsAt = KnockoutStage.ROUND_OF_128;
        // since the competition we create starts at the round of 128, it MUST contain these
        // knockout stages, in this order
        List<Pair<KnockoutStage, List<KnockoutSlot>>> slotsPerStage = List.of(
                Pair.of(KnockoutStage.ROUND_OF_128, generateEmptySlots(64)),
                Pair.of(KnockoutStage.ROUND_OF_64, generateEmptySlots(32)),
                Pair.of(KnockoutStage.ROUND_OF_32, generateEmptySlots(16)),
                Pair.of(KnockoutStage.ROUND_OF_16, generateEmptySlots(8)),
                Pair.of(KnockoutStage.QUARTER_FINAL, generateEmptySlots(4)),
                Pair.of(KnockoutStage.SEMI_FINAL, generateEmptySlots(2)),
                Pair.of(KnockoutStage.FINAL, generateEmptySlots(1))
        );

        var dtoCompetition = TestUpsertCompetitionDto.builder()
                .knockoutPhase(new UpsertCompetitionDto.UpsertKnockoutPhaseDto(startsAt.name()))
                .build();
        var expectedCompetition = new Competition(
                dtoCompetition.name(),
                dtoCompetition.season(),
                dtoCompetition.logoUrl()
        );
        var expectedKnockoutPhase = new KnockoutPhase();
        var stages = slotsPerStage.stream().map(pair -> {
            var stage = new Stage(pair.getFirst());
            stage.setSlots(pair.getSecond());
            return stage;
        }).toList();
        expectedKnockoutPhase.setStages(stages);
        expectedCompetition.setKnockoutPhase(expectedKnockoutPhase);

        // given
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
    @DisplayName("createCompetition correctly constructs a knockout competition when it starts at the round of 16")
    public void createCompetition_StartsAtRoundOf16_ConstructsEntityAndSaves() throws CompetitionInvalidException {
        var startsAt = KnockoutStage.ROUND_OF_16;
        // since the competition we create starts at the round of 16, it MUST contain these
        // knockout stages, in this order
        List<Pair<KnockoutStage, List<KnockoutSlot>>> slotsPerStage = List.of(
                Pair.of(KnockoutStage.ROUND_OF_16, generateEmptySlots(8)),
                Pair.of(KnockoutStage.QUARTER_FINAL, generateEmptySlots(4)),
                Pair.of(KnockoutStage.SEMI_FINAL, generateEmptySlots(2)),
                Pair.of(KnockoutStage.FINAL, generateEmptySlots(1))
        );

        var dtoCompetition = TestUpsertCompetitionDto.builder()
                .knockoutPhase(new UpsertCompetitionDto.UpsertKnockoutPhaseDto(startsAt.name()))
                .build();
        var expectedCompetition = new Competition(
                dtoCompetition.name(),
                dtoCompetition.season(),
                dtoCompetition.logoUrl()
        );
        var expectedKnockoutPhase = new KnockoutPhase();
        var stages = slotsPerStage.stream().map(pair -> {
            var stage = new Stage(pair.getFirst());
            stage.setSlots(pair.getSecond());
            return stage;
        }).toList();
        expectedKnockoutPhase.setStages(stages);
        expectedCompetition.setKnockoutPhase(expectedKnockoutPhase);

        // given
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
    @DisplayName("createCompetition correctly constructs a knockout competition when it starts at the semi-final")
    public void createCompetition_StartsAtSemiFinal_ConstructsEntityAndSaves() throws CompetitionInvalidException {
        var startsAt = KnockoutStage.SEMI_FINAL;
        // since the competition we create starts at the semifinal, it MUST contain these
        // knockout stages, in this order
        List<Pair<KnockoutStage, List<KnockoutSlot>>> slotsPerStage = List.of(
                Pair.of(KnockoutStage.SEMI_FINAL, generateEmptySlots(2)),
                Pair.of(KnockoutStage.FINAL, generateEmptySlots(1))
        );

        var dtoCompetition = TestUpsertCompetitionDto.builder()
                .knockoutPhase(new UpsertCompetitionDto.UpsertKnockoutPhaseDto(startsAt.name()))
                .build();
        var expectedCompetition = new Competition(
                dtoCompetition.name(),
                dtoCompetition.season(),
                dtoCompetition.logoUrl()
        );
        var expectedKnockoutPhase = new KnockoutPhase();
        var stages = slotsPerStage.stream().map(pair -> {
            var stage = new Stage(pair.getFirst());
            stage.setSlots(pair.getSecond());
            return stage;
        }).toList();
        expectedKnockoutPhase.setStages(stages);
        expectedCompetition.setKnockoutPhase(expectedKnockoutPhase);

        // given
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

    @Test
    @DisplayName("findMatchesByRound throws when the competition does not exist")
    public void findMatchesByRound_CompetitionNotFound_Throws() {
        var competitionId = UUID.randomUUID();
        var round = 1;

        // given
        given(competitionRepository.findById(eq(competitionId))).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            competitionService.findMatchesByRound(competitionId, round);
        }).getMessage();

        // then
        assertEquals(String.format("competition %s could not be found", competitionId), message);
    }

    @Test
    @DisplayName("findMatchesByRound throws when the competition does not have a league phase")
    public void findMatchesByRound_LeaguePhaseNotFound_Throws() {
        var competitionId = UUID.randomUUID();
        var round = 1;

        var competition = TestCompetition.builder().leaguePhase(null).build();

        // given
        given(competitionRepository.findById(eq(competitionId))).willReturn(Optional.of(competition));

        // when
        String message = assertThrows(CompetitionPhaseNotFoundException.class, () -> {
            competitionService.findMatchesByRound(competitionId, round);
        }).getMessage();

        // then
        assertEquals("competition does not have the phase required to execute this action", message);
    }

    @Test
    @DisplayName("findMatchesByRound throws when the competition does not have a given round")
    public void findMatchesByRound_RoundsNotFound_Throws() {
        var competitionId = UUID.randomUUID();

        // maxRounds set to 34 means that only rounds (1, 34) exist
        var maxRounds = 34;
        var competition = TestCompetition.builder().leaguePhase(new LeaguePhase(List.of(), List.of(), maxRounds)).build();

        // given
        given(competitionRepository.findById(eq(competitionId))).willReturn(Optional.of(competition));

        for (var incorrectRound = maxRounds + 1; incorrectRound < 50; incorrectRound++) {
            // when
            int finalIncorrectRound = incorrectRound;
            String message = assertThrows(CompetitionRoundNotFoundException.class, () -> {
                competitionService.findMatchesByRound(competitionId, finalIncorrectRound);
            }).getMessage();

            // then
            assertEquals(String.format("round %s could not be found", incorrectRound), message);
        }
    }

    @Test
    @DisplayName("findMatchesByRound correctly calls the external endpoint")
    public void findMatchesByRound_CompetitionFoundAndValidRound_CorrectlyCallsClient() throws Exception {
        var competitionId = UUID.randomUUID();

        // maxRounds set to 34 means that only rounds (1, 34) exist
        var maxRounds = 34;
        var competition = TestCompetition.builder().leaguePhase(new LeaguePhase(List.of(), List.of(), maxRounds)).build();

        // given
        given(competitionRepository.findById(eq(competitionId))).willReturn(Optional.of(competition));

        for (var round = 1; round <= maxRounds; round++) {
            var matchId = UUID.randomUUID();
            List<LeagueSlot> leagueSlots = List.of(new LeagueSlot(new CompetitionMatch(matchId), competitionId, round));

            doReturn(leagueSlots)
                    .when(leagueSlotRepository)
                    .findAllByCompetitionIdAndRoundAndDeletedFalse(eq(competitionId), eq(round));

            // when
            var result = competitionService.findMatchesByRound(competitionId, round);

            // then
            var expectedMatchIds = List.of(matchId);
            assertEquals(0, result.size());
            verify(matchServiceClient).getMatchesById(eq(expectedMatchIds));
        }
    }

    @Test
    @DisplayName("assignMatchesToRound throws when the competition does not have a league phase")
    public void assignMatchesToRound_LeaguePhaseNotFound_Throws() {
        var competitionId = UUID.randomUUID();
        var round = 1;
        var matchIdsToAssign = List.of(UUID.randomUUID());

        var competition = TestCompetition.builder().leaguePhase(null).build();

        // given
        given(competitionRepository.findById(eq(competitionId))).willReturn(Optional.of(competition));

        // when
        String message = assertThrows(CompetitionPhaseNotFoundException.class, () -> {
            competitionService.assignMatchesToRound(competitionId, round, matchIdsToAssign);
        }).getMessage();

        // then
        assertEquals("competition does not have the phase required to execute this action", message);
    }

    @Test
    @DisplayName("assignMatchesToRound throws when the competition does not have a given round")
    public void assignMatchesToRound_RoundsNotFound_Throws() {
        var competitionId = UUID.randomUUID();
        var matchIdsToAssign = List.of(UUID.randomUUID());

        // maxRounds set to 34 means that only rounds (1, 34) exist
        var maxRounds = 34;
        var competition = TestCompetition.builder().leaguePhase(new LeaguePhase(List.of(), List.of(), maxRounds)).build();

        // given
        given(competitionRepository.findById(eq(competitionId))).willReturn(Optional.of(competition));

        for (var incorrectRound = maxRounds + 1; incorrectRound < 50; incorrectRound++) {
            // when
            int finalIncorrectRound = incorrectRound;
            String message = assertThrows(CompetitionRoundNotFoundException.class, () -> {
                competitionService.assignMatchesToRound(competitionId, finalIncorrectRound, matchIdsToAssign);
            }).getMessage();

            // then
            assertEquals(String.format("round %s could not be found", incorrectRound), message);
        }
    }

    @Test
    @DisplayName("assignMatchesToRound throws when the competition's round is not empty")
    public void assignMatchesToRound_RoundsNotEmpty_Throws() {
        var round = 1;
        var competitionId = UUID.randomUUID();
        var matchIdsToAssign = List.of(UUID.randomUUID());

        // maxRounds set to 34 means that only rounds (1, 34) exist
        var maxRounds = 34;
        var competition = TestCompetition.builder().leaguePhase(new LeaguePhase(List.of(), List.of(), maxRounds)).build();

        var leagueSlots = List.of(new LeagueSlot(new CompetitionMatch(UUID.randomUUID()), competitionId, round));

        // given
        given(competitionRepository.findById(eq(competitionId))).willReturn(Optional.of(competition));
        given(leagueSlotRepository.findAllByCompetitionIdAndRoundAndDeletedFalse(eq(competitionId), eq(round)))
                .willReturn(leagueSlots);

        // when
        String message = assertThrows(CompetitionRoundNotEmptyException.class, () -> {
            competitionService.assignMatchesToRound(competitionId, round, matchIdsToAssign);
        }).getMessage();

        // then
        assertEquals("only an empty round can have matches assigned to it", message);
    }

    private static class UnassignedMatchIdsMatcher implements ArgumentMatcher<Iterable<UnassignedMatch.UnassignedMatchId>> {
        private final List<UnassignedMatch.UnassignedMatchId> expectedUnassignedMatchIds;

        public UnassignedMatchIdsMatcher(List<UnassignedMatch.UnassignedMatchId> expectedIds) {
            this.expectedUnassignedMatchIds = expectedIds;
        }

        @Override
        public boolean matches(Iterable<UnassignedMatch.UnassignedMatchId> unassignedMatchIds) {
            for (var unassignedMatchId : unassignedMatchIds) {
                var mId = unassignedMatchId.getMatchId();
                var cId = unassignedMatchId.getCompetitionId();
                var requestedPresent = expectedUnassignedMatchIds.stream().anyMatch(
                        requested -> requested.getMatchId().equals(mId) && requested.getCompetitionId().equals(cId)
                );
                if (!requestedPresent) {
                    return false;
                }
            }
            return true;
        }
    }

    @Test
    @DisplayName("assignMatchesToRound throws when some matches requested to be assigned are already assigned")
    public void assignMatchesToRound_SomeMatchesAlreadyAssigned_Throws() {
        var round = 1;
        var competitionId = UUID.randomUUID();

        var unassignedMatchId = UUID.randomUUID();
        var unassignedMatch = new UnassignedMatch(unassignedMatchId, competitionId);

        var alreadyAssignedMatchId = UUID.randomUUID();
        // one of the requested matchIds is already assigned
        var matchIdsToAssign = List.of(unassignedMatchId, alreadyAssignedMatchId);

        // maxRounds set to 34 means that only rounds (1, 34) exist
        var maxRounds = 34;
        var competition = TestCompetition.builder().leaguePhase(new LeaguePhase(List.of(), List.of(), maxRounds)).build();

        // given
        given(competitionRepository.findById(eq(competitionId))).willReturn(Optional.of(competition));
        given(leagueSlotRepository.findAllByCompetitionIdAndRoundAndDeletedFalse(eq(competitionId), eq(round)))
                .willReturn(List.of());
        var expectedUnassignedMatchIds = matchIdsToAssign
                .stream().map(id -> new UnassignedMatch.UnassignedMatchId(id, competitionId))
                .toList();
        // return only one unassigned match, simulating one match being already assigned
        given(unassignedMatchRepository.findAllByIdIsInAndAssignedFalse(
                argThat(new UnassignedMatchIdsMatcher(expectedUnassignedMatchIds))))
                .willReturn(List.of(unassignedMatch));

        // when
        String message = assertThrows(CompetitionMatchAssignmentException.class, () -> {
            competitionService.assignMatchesToRound(competitionId, round, matchIdsToAssign);
        }).getMessage();

        // then
        assertEquals(String.format("matches [%s] could not be assigned", alreadyAssignedMatchId), message);
    }

    @Test
    @DisplayName("assignMatchesToRound correctly assigns matches when none of the requested matches were previously assigned")
    public void assignMatchesToRound_NoneMatchesAssigned_CorrectlyAssigns() throws Exception {
        var competitionId = UUID.randomUUID();

        var unassignedMatch0 = new UnassignedMatch(UUID.randomUUID(), competitionId);
        var unassignedMatch1 = new UnassignedMatch(UUID.randomUUID(), competitionId);
        var unassignedMatch2 = new UnassignedMatch(UUID.randomUUID(), competitionId);

        var unassignedMatches = List.of(unassignedMatch0, unassignedMatch1, unassignedMatch2);
        var matchIdsToAssign = unassignedMatches.stream().map(um -> um.getId().getMatchId()).toList();

        // maxRounds set to 34 means that only rounds (1, 34) exist
        var maxRounds = 34;
        var competition = TestCompetition.builder().leaguePhase(new LeaguePhase(List.of(), List.of(), maxRounds)).build();

        for (var correctRound = 1; correctRound <= maxRounds; correctRound++) {
            // given
            doReturn(Optional.of(competition)).when(competitionRepository).findById(eq(competitionId));
            doReturn(List.of()).when(leagueSlotRepository).findAllByCompetitionIdAndRoundAndDeletedFalse(
                    eq(competitionId), eq(correctRound)
            );
            var expectedUnassignedMatchIds = matchIdsToAssign
                    .stream().map(id -> new UnassignedMatch.UnassignedMatchId(id, competitionId))
                    .toList();
            doReturn(unassignedMatches).when(unassignedMatchRepository)
                    .findAllByIdIsInAndAssignedFalse(
                            argThat(new UnassignedMatchIdsMatcher(expectedUnassignedMatchIds))
                    );

            // when
            competitionService.assignMatchesToRound(competitionId, correctRound, matchIdsToAssign);

            // then
            // all unassigned matches should be turned into league slot entries
            int finalCorrectRound = correctRound;
            verify(leagueSlotRepository).saveAll(argThat(leagueSlots -> {
                for (var slot : leagueSlots) {
                    var slotCorrect =
                            slot.getCompetitionId().equals(competitionId) &&
                            slot.getRound() == finalCorrectRound &&
                            matchIdsToAssign.contains(slot.getMatch().getMatchId());
                    if (!slotCorrect) {
                        return false;
                    }
                }
                return true;
            }));
        }

        // all matches should be marked as assigned
        verify(unassignedMatchRepository, times(maxRounds)).saveAll(argThat(unassignedMatch -> {
            for (var um : unassignedMatch) {
                if (!um.isAssigned()) {
                    return false;
                }
            }
            return true;
        }));
    }

    @Test
    @DisplayName("unassignMatchesFromRound does nothing if there are no matches in a particular round")
    public void unassignMatchesFromRound_RoundEmpty_NoOperation() {
        var competitionId = UUID.randomUUID();
        var round = 1;

        // given
        given(leagueSlotRepository.findAllByCompetitionIdAndRoundAndDeletedFalse(eq(competitionId), eq(round)))
                .willReturn(List.of());

        // when
        competitionService.unassignMatchesFromRound(competitionId, round);

        // then
        verify(leagueSlotRepository, never()).saveAll(any());
        verify(unassignedMatchRepository, never()).findAllById(any());
        verify(unassignedMatchRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("unassignMatchesFromRound unassigns matches from round and resets unassigned statuses")
    public void unassignMatchesFromRound_RoundNotEmpty_UnassignsMatches() {
        var competitionId = UUID.randomUUID();
        var round = 1;

        // this match must be assigned, since it
        var matchId = UUID.randomUUID();
        var assignedMatch = new UnassignedMatch(matchId, competitionId);
        assignedMatch.setAssigned(true);

        var unassignedMatches = List.of(assignedMatch);
        var leagueSlots = List.of(new LeagueSlot(new CompetitionMatch(matchId), competitionId, round));

        // given
        given(leagueSlotRepository.findAllByCompetitionIdAndRoundAndDeletedFalse(eq(competitionId), eq(round)))
                .willReturn(leagueSlots);
        given(unassignedMatchRepository.findAllById(any())).willReturn(unassignedMatches);

        // when
        competitionService.unassignMatchesFromRound(competitionId, round);

        // then
        verify(leagueSlotRepository).saveAll(argThat(lSlots -> {
            // every league slot is marked as deleted
            for (var leagueSlot : lSlots) {
                if (!leagueSlot.isDeleted()) {
                    return false;
                }
            }
            return true;
        }));
        verify(unassignedMatchRepository).saveAll(argThat(uMatches -> {
            // all matches are marked as unassigned
            for (var uMatch : uMatches) {
                if (uMatch.isAssigned()) {
                    return false;
                }
            }
            return true;
        }));
    }

    @Test
    @DisplayName("findKnockoutPhase throws when the competition does not exist")
    public void findKnockoutPhase_CompetitionNotFound_Throws() {
        var competitionId = UUID.randomUUID();

        // given
        given(competitionRepository.findById(eq(competitionId))).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            competitionService.findKnockoutPhase(competitionId);
        }).getMessage();

        // then
        assertEquals(String.format("competition %s could not be found", competitionId), message);
    }

    @Test
    @DisplayName("findKnockoutPhase throws when the competition does not have a knockout phase")
    public void findKnockoutPhase_KnockoutPhaseNotFound_Throws() {
        var competitionId = UUID.randomUUID();

        var competition = TestCompetition.builder().knockoutPhase(null).build();

        // given
        given(competitionRepository.findById(eq(competitionId))).willReturn(Optional.of(competition));

        // when
        String message = assertThrows(CompetitionPhaseNotFoundException.class, () -> {
            competitionService.findKnockoutPhase(competitionId);
        }).getMessage();

        // then
        assertEquals("competition does not have the phase required to execute this action", message);
    }

    @Test
    @DisplayName("findKnockoutPhase does not fetch any data from external services if all knockout slots are empty")
    public void findKnockoutPhase_AllSlotsEmpty_ReturnsWithoutFetchingExternalData() throws Exception {
        // create a knockout phase where all slots are EMPTY
        Stage semifinalStage = new Stage(KnockoutStage.SEMI_FINAL);
        semifinalStage.setSlots(List.of(new KnockoutSlot.Empty(), new KnockoutSlot.Empty()));
        Stage finalStage = new Stage(KnockoutStage.FINAL);
        finalStage.setSlots(List.of(new KnockoutSlot.Empty()));
        KnockoutPhase knockoutPhase = new KnockoutPhase(List.of(semifinalStage, finalStage));

        Competition competition = TestCompetition.builder().knockoutPhase(knockoutPhase).build();
        var competitionId = competition.getId();

        // given
        given(competitionRepository.findById(eq(competitionId))).willReturn(Optional.of(competition));

        // when
        var result = competitionService.findKnockoutPhase(competitionId);

        // then
        var receivedSlots = result.stages().stream().flatMap(s -> s.slots().stream()).toList();
        assertEquals(3, receivedSlots.stream().filter(s -> s.getType().equals("EMPTY")).count());
        verify(matchServiceClient, never()).getMatchesById(any());
        verify(matchServiceClient, never()).getTeamByTeamIds(any(), any());
    }

    @Test
    @DisplayName("findKnockoutPhase does fetch team data from external services if all knockout slots are byes")
    public void findKnockoutPhase_AllSlotsBye_ReturnsWithFetchingExternalData() throws Exception {
        var team0Id = UUID.randomUUID();
        var team1Id = UUID.randomUUID();
        var team2Id = UUID.randomUUID();
        var expectedFetchedTeamIds = List.of(team0Id, team1Id, team2Id);
        var teamDetails = expectedFetchedTeamIds.stream().map(id -> new TeamDetailsDto(id, "Team", "")).toList();
        var page = new PageImpl<>(teamDetails);

        // create a knockout phase where all slots are BYEs
        Stage semifinalStage = new Stage(KnockoutStage.SEMI_FINAL);
        semifinalStage.setSlots(List.of(new KnockoutSlot.Bye(team0Id), new KnockoutSlot.Bye(team1Id)));
        Stage finalStage = new Stage(KnockoutStage.FINAL);
        finalStage.setSlots(List.of(new KnockoutSlot.Bye(team2Id)));
        KnockoutPhase knockoutPhase = new KnockoutPhase(List.of(semifinalStage, finalStage));

        Competition competition = TestCompetition.builder().knockoutPhase(knockoutPhase).build();
        var competitionId = competition.getId();

        // given
        given(competitionRepository.findById(eq(competitionId))).willReturn(Optional.of(competition));
        given(matchServiceClient.getTeamByTeamIds(
                argThat(teamIds -> teamIds.containsAll(expectedFetchedTeamIds) && teamIds.size() == expectedFetchedTeamIds.size()),
                any()
        )).willReturn(page);

        // when
        var result = competitionService.findKnockoutPhase(competitionId);

        // then
        var receivedSlots = result.stages().stream().flatMap(s -> s.slots().stream()).toList();
        assertEquals(3, receivedSlots.stream().filter(s -> s.getType().equals("BYE")).count());
        verify(matchServiceClient, never()).getMatchesById(any());
    }

    private CompactMatchDto createTestCompactMatchDto(UUID id) {
        return new CompactMatchDto(
                id, "", LocalDateTime.now(), "", UUID.randomUUID(), LocalDateTime.now(),
                new CompactMatchDto.TeamDto(UUID.randomUUID(), "", ""),
                new CompactMatchDto.TeamDto(UUID.randomUUID(), "", ""),
                new CompactMatchDto.ScoreInfoDto(0, 0),
                new CompactMatchDto.ScoreInfoDto(0, 0),
                new CompactMatchDto.ScoreInfoDto(0, 0),
                new CompactMatchDto.RedCardInfoDto(0, 0)
        );
    }

    @Test
    @DisplayName("findKnockoutPhase does fetch match data from external services if all knockout slots are taken")
    public void findKnockoutPhase_AllSlotsTaken_ReturnsWithFetchingExternalData() throws Exception {
        var match0Id = UUID.randomUUID();
        var match1Id = UUID.randomUUID();
        var match2Id = UUID.randomUUID();
        var expectedFetchedMatchIds = List.of(match0Id, match1Id, match2Id);
        var matchDetails = expectedFetchedMatchIds.stream().map(this::createTestCompactMatchDto).toList();

        // create a knockout phase where all slots are TAKEN
        Stage semifinalStage = new Stage(KnockoutStage.SEMI_FINAL);
        semifinalStage.setSlots(List.of(
                new KnockoutSlot.Taken(new CompetitionMatch(match0Id)),
                new KnockoutSlot.Taken(new CompetitionMatch(match1Id)))
        );
        Stage finalStage = new Stage(KnockoutStage.FINAL);
        finalStage.setSlots(List.of(new KnockoutSlot.Taken(new CompetitionMatch(match2Id))));
        KnockoutPhase knockoutPhase = new KnockoutPhase(List.of(semifinalStage, finalStage));

        Competition competition = TestCompetition.builder().knockoutPhase(knockoutPhase).build();
        var competitionId = competition.getId();

        // given
        given(competitionRepository.findById(eq(competitionId))).willReturn(Optional.of(competition));
        given(matchServiceClient.getMatchesById(
                argThat(matchIds -> matchIds.containsAll(expectedFetchedMatchIds) && matchIds.size() == expectedFetchedMatchIds.size())
        )).willReturn(matchDetails);

        // when
        var result = competitionService.findKnockoutPhase(competitionId);

        // then
        var receivedSlots = result.stages().stream().flatMap(s -> s.slots().stream()).toList();
        assertEquals(3, receivedSlots.stream().filter(s -> s.getType().equals("TAKEN")).count());
        verify(matchServiceClient, never()).getTeamByTeamIds(any(), any());
    }
    @Test
    @DisplayName("findKnockoutPhase maintains the ordering of stages and slots during processing")
    public void findKnockoutPhase_MultipleSlotTypes_ReturnsMaintainingShape() throws Exception {
        var team0Id = UUID.randomUUID();
        var team1Id = UUID.randomUUID();
        var team2Id = UUID.randomUUID();
        var expectedFetchedTeamIds = List.of(team0Id, team1Id, team2Id);
        var teamDetails = expectedFetchedTeamIds.stream().map(id -> new TeamDetailsDto(id, "Team", "")).toList();
        var teamDetailsPage = new PageImpl<>(teamDetails);

        var match0Id = UUID.randomUUID();
        var match1Id = UUID.randomUUID();
        var expectedFetchedMatchIds = List.of(match0Id, match1Id);
        var matchDetails = expectedFetchedMatchIds.stream().map(this::createTestCompactMatchDto).toList();
        // create a competition with three stages QUARTER_FINAL, SEMI_FINAL, FINAL
        // where each stage has mixed types of slots
        //
        //  QF      SF      FINAL
        //  -----------------------
        //  EMPTY
        //          TAKEN
        //  BYE
        //                  EMPTY
        //  TAKEN
        //          BYE
        //  BYE
        Stage quarterfinalStage = new Stage(KnockoutStage.QUARTER_FINAL);
        quarterfinalStage.setSlots(List.of(
                new KnockoutSlot.Empty(),
                new KnockoutSlot.Bye(team0Id),
                new KnockoutSlot.Taken(new CompetitionMatch(match0Id)),
                new KnockoutSlot.Bye(team1Id)
        ));
        Stage semifinalStage = new Stage(KnockoutStage.SEMI_FINAL);
        semifinalStage.setSlots(List.of(
                new KnockoutSlot.Taken(new CompetitionMatch(match1Id)),
                new KnockoutSlot.Bye(team2Id)
        ));
        Stage finalStage = new Stage(KnockoutStage.FINAL);
        finalStage.setSlots(List.of(new KnockoutSlot.Empty()));
        KnockoutPhase knockoutPhase = new KnockoutPhase(List.of(quarterfinalStage, semifinalStage, finalStage));

        Competition competition = TestCompetition.builder().knockoutPhase(knockoutPhase).build();
        var competitionId = competition.getId();

        // given
        given(competitionRepository.findById(eq(competitionId))).willReturn(Optional.of(competition));
        given(matchServiceClient.getTeamByTeamIds(
                argThat(teamIds -> teamIds.containsAll(expectedFetchedTeamIds) && teamIds.size() == expectedFetchedTeamIds.size()),
                any()
        )).willReturn(teamDetailsPage);
        given(matchServiceClient.getMatchesById(
                argThat(matchIds -> matchIds.containsAll(expectedFetchedMatchIds) && matchIds.size() == expectedFetchedMatchIds.size())
        )).willReturn(matchDetails);

        // when
        var result = competitionService.findKnockoutPhase(competitionId);

        // then
        var receivedQuarterfinalStage = result.stages().get(0);
        assertEquals("QUARTER_FINAL", receivedQuarterfinalStage.stage());
        var receivedQuarterfinalSlots = receivedQuarterfinalStage
                .slots()
                .stream()
                .map(KnockoutPhaseDto.KnockoutSlotDto::getType)
                .toList();
        assertEquals(List.of("EMPTY", "BYE", "TAKEN", "BYE"), receivedQuarterfinalSlots);

        var receivedSemifinalStage = result.stages().get(1);
        assertEquals("SEMI_FINAL", receivedSemifinalStage.stage());
        var receivedSemifinalSlots = receivedSemifinalStage
                .slots()
                .stream()
                .map(KnockoutPhaseDto.KnockoutSlotDto::getType)
                .toList();
        assertEquals(List.of("TAKEN", "BYE"), receivedSemifinalSlots);

        var receivedFinalStage = result.stages().get(2);
        assertEquals("FINAL", receivedFinalStage.stage());
        var receivedFinalSlots = receivedFinalStage
                .slots()
                .stream()
                .map(KnockoutPhaseDto.KnockoutSlotDto::getType)
                .toList();
        assertEquals(List.of("EMPTY"), receivedFinalSlots);
    }
}
