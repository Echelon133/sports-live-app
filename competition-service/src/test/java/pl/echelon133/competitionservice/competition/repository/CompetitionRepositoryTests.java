package pl.echelon133.competitionservice.competition.repository;

import pl.echelon133.competitionservice.competition.model.CompetitionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import pl.echelon133.competitionservice.competition.TestCompetition;
import pl.echelon133.competitionservice.competition.model.Competition;
import pl.echelon133.competitionservice.competition.model.PlayerStats;
import pl.echelon133.competitionservice.competition.model.PlayerStatsDto;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Disable kubernetes during tests
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@DataJpaTest
public class CompetitionRepositoryTests {

    private final CompetitionRepository competitionRepository;
    private final PlayerStatsRepository playerStatsRepository;

    @Autowired
    public CompetitionRepositoryTests(CompetitionRepository competitionRepository, PlayerStatsRepository playerStatsRepository) {
        this.competitionRepository = competitionRepository;
        this.playerStatsRepository = playerStatsRepository;
    }

    private static void assertEntityAndDtoEqual(Competition entity, CompetitionDto dto) {
        assertEquals(entity.getId(), dto.getId());
        assertEquals(entity.getName(), dto.getName());
        assertEquals(entity.getSeason(), dto.getSeason());
        assertEquals(entity.getLogoUrl(), dto.getLogoUrl());
    }

    @Test
    @DisplayName("findCompetitionById native query finds empty when the competition does not exist")
    public void findCompetitionById_CompetitionDoesNotExist_IsEmpty() {
        var id = UUID.randomUUID();

        // when
        var competitionDto = competitionRepository.findCompetitionById(id);

        // then
        assertTrue(competitionDto.isEmpty());
    }

    @Test
    @DisplayName("findCompetitionById native query does not fetch competitions marked as deleted")
    public void findCompetitionById_CompetitionMarkedAsDeleted_IsEmpty() {
        var competitionToDelete = TestCompetition.builder().deleted(true).build();
        var saved = competitionRepository.save(competitionToDelete);

        // when
        var competitionDto = competitionRepository.findCompetitionById(saved.getId());

        // then
        assertTrue(competitionDto.isEmpty());
    }

    @Test
    @DisplayName("findCompetitionById native query finds competition when the competition exists")
    public void findCompetitionById_CompetitionExists_IsPresent() {
        var competition = TestCompetition.builder().build();
        var saved = competitionRepository.save(competition);

        // when
        var competitionDto = competitionRepository.findCompetitionById(saved.getId());

        // then
        assertTrue(competitionDto.isPresent());
        assertEntityAndDtoEqual(competition, competitionDto.get());
    }

    @Test
    @DisplayName("markCompetitionAsDeleted native query only affects the competition with specified id")
    public void markCompetitionAsDeleted_SpecifiedCompetitionId_OnlyMarksSpecifiedCompetition() {
        var c1 = competitionRepository.save(TestCompetition.builder().build());
       competitionRepository.save(TestCompetition.builder().build());

       // when
        var countDeleted = competitionRepository.markCompetitionAsDeleted(c1.getId());
        var competitionDto = competitionRepository.findCompetitionById(c1.getId());

        // then
        assertEquals(1, countDeleted);
        assertTrue(competitionDto.isEmpty());
    }

    @Test
    @DisplayName("markCompetitionAsDeleted native query only affects non deleted competitions")
    public void markCompetitionAsDeleted_CompetitionAlreadyMarkedAsDeleted_IsNotTouchedByQuery() {
        var competitionToDelete = TestCompetition.builder().deleted(true).build();
        var saved = competitionRepository.save(competitionToDelete);

        // when
        var countDeleted = competitionRepository.markCompetitionAsDeleted(saved.getId());

        // then
        assertEquals(0, countDeleted);
    }

    @Test
    @DisplayName("findAllByNameContaining native query only finds results which contain the specified phrase")
    public void findAllByNameContaining_MultipleCompetitions_OnlyFindsMatchingCompetitions() {
        competitionRepository.save(TestCompetition.builder().name("Serie A").build());
        competitionRepository.save(TestCompetition.builder().name("Serie B").build());
        var saved = competitionRepository.save(TestCompetition.builder().name("La Liga").build());

        // when
        Page<CompetitionDto> result = competitionRepository.findAllByNameContaining("Liga", Pageable.ofSize(10));

        // then
        assertEquals(1, result.getTotalElements());
        assertEntityAndDtoEqual(saved, result.getContent().get(0));
    }

    @Test
    @DisplayName("findAllByNameContaining native query is case-insensitive")
    public void findAllByNameContaining_MultipleCompetitions_SearchIsCaseInsensitive() {
        competitionRepository.save(TestCompetition.builder().name("Serie A").build());
        competitionRepository.save(TestCompetition.builder().name("serie B").build());
        competitionRepository.save(TestCompetition.builder().name("la liga").build());

        // when
        Page<CompetitionDto> result = competitionRepository.findAllByNameContaining("serie", Pageable.ofSize(10));

        // then
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().anyMatch(p -> p.getName().equals("Serie A")));
        assertTrue(result.getContent().stream().anyMatch(p -> p.getName().equals("serie B")));
    }

    @Test
    @DisplayName("findAllByNameContaining native query only finds non-deleted results which contain the specified phrase")
    public void findAllByNameContaining_SomeDeletedCompetitions_OnlyFindsMatchingNonDeletedCompetitions() {
        var competitionToDelete = TestCompetition.builder().name("Serie A").deleted(true).build();
        competitionRepository.save(competitionToDelete);
        competitionRepository.save(TestCompetition.builder().name("Serie B").build());
        competitionRepository.save(TestCompetition.builder().name("La Liga").build());

        // when
        Page<CompetitionDto> result = competitionRepository.findAllByNameContaining("Serie", Pageable.ofSize(10));

        // then
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().stream().anyMatch(p -> p.getName().equals("Serie B")));
    }

    @Test
    @DisplayName("findAllByNameContaining native query takes page size information from Pageable into account")
    public void findAllByNameContaining_PageableCustomPageSize_UsesPageableInfo() {
        // when
        Page<CompetitionDto> result = competitionRepository.findAllByNameContaining("test", Pageable.ofSize(8));

        // then
        assertEquals(8, result.getPageable().getPageSize());
    }

    @Test
    @DisplayName("findAllByNameContaining native query takes page number information from Pageable into account")
    public void findAllByNameContaining_PageableCustomPageNumber_UsesPageableInfo() {
        var pageable = PageRequest.ofSize(6).withPage(5);

        // when
        Page<CompetitionDto> result = competitionRepository.findAllByNameContaining("test", pageable);

        // then
        var receivedPageable = result.getPageable();
        assertEquals(6, receivedPageable.getPageSize());
        assertEquals(5, receivedPageable.getPageNumber());
    }

    @Test
    @DisplayName("findAllPinned native query only fetches non-deleted competitions which are marked as pinned")
    public void findAllPinned_MultipleCombinationsOfCompetitions_OnlyFindsPinnedNonDeletedCompetitions() {
        // competitions which should NOT be returned by the tested query
        competitionRepository.saveAll(List.of(
                // 1. deleted, not pinned
                TestCompetition.builder().deleted(true).build(),
                // 2. deleted, pinned
                TestCompetition.builder().deleted(true).pinned(true).build(),
                // 3. non-deleted, not pinned
                TestCompetition.builder().build())
        );

        // the only competition which should be returned by the query
        // 4. non-deleted, pinned
        var expectedCompetition = competitionRepository.save(
                TestCompetition.builder().name("Competition4").pinned(true).build()
        );

        // when
        var result = competitionRepository.findAllPinned();

        // then
        assertEquals(1, result.size());
        assertEntityAndDtoEqual(expectedCompetition, result.get(0));
    }

    @Test
    @DisplayName("findPlayerStats native query finds zero player stats when the competition does not exist")
    public void findPlayerStats_CompetitionNotFound_ContentIsEmpty() {
        var competitionId = UUID.randomUUID();

        // when
        var page = competitionRepository.findPlayerStats(competitionId, Pageable.unpaged());

        // then
        assertEquals(0, page.getNumberOfElements());
    }

    @Test
    @DisplayName("findPlayerStats native query only fetches players stats belonging to a particular competition")
    public void findPlayerStats_MultipleCompetitionsExist_OnlyFindsExpectedPlayerStats() {
        // setup competitionA
        var competitionA = TestCompetition.builder()
                .name("Competition A")
                .build();

        // setup competitionB
        var competitionB = TestCompetition.builder()
                .name("Competition B")
                .build();

        // setup competitionC
        var competitionC = TestCompetition.builder()
                .name("Competition C")
                .build();

        competitionRepository.saveAll(List.of(competitionA, competitionB, competitionC));

        var competitionAPlayer = createPlayerStats("Player A", 10, 2, 1, 0, competitionA);
        var competitionBPlayer = createPlayerStats("Player B", 8, 5, 3, 1, competitionB);
        var competitionCPlayer = createPlayerStats("Player C", 3, 1, 0, 1, competitionC);
        playerStatsRepository.saveAll(List.of(competitionAPlayer, competitionBPlayer, competitionCPlayer));

        // when
        var page = competitionRepository.findPlayerStats(competitionA.getId(), Pageable.unpaged());

        // then
        assertEquals(1, page.getNumberOfElements());
        var receivedStats = page.getContent().get(0);
        assertEntityAndDtoEqual(competitionAPlayer, receivedStats);
    }

    @Test
    @DisplayName("findPlayerStats native query sorts player statistics by goals, then assists")
    public void findPlayerStats_MultiplePlayerStats_ResultsSortedByGoalsAndAssists() {
        var competition = TestCompetition.builder().build();
        competitionRepository.save(competition);

        var playerA = createPlayerStats("Player A", 10, 2, 1, 0, competition);
        var playerB = createPlayerStats("Player B", 10, 3, 3, 1, competition);
        var playerC = createPlayerStats("Player C", 10, 4, 0, 1, competition);
        var playerD = createPlayerStats("Player D", 9, 0, 0, 1, competition);
        var playerE = createPlayerStats("Player E", 9, 1, 0, 1, competition);
        var playerF = createPlayerStats("Player F", 11, 0, 0, 1, competition);
        var playerG = createPlayerStats("Player G", 10, 1, 0, 1, competition);
        playerStatsRepository.saveAll(List.of(playerA, playerB, playerC, playerD, playerE, playerF, playerG));

        var expectedOrder = List.of(playerF, playerC, playerB, playerA, playerG, playerE, playerD);

        // when
        var page = competitionRepository.findPlayerStats(competition.getId(), Pageable.unpaged());

        // then
        assertEquals(7, page.getNumberOfElements());
        var receivedStats = page.getContent();
        for (int i = 0; i < receivedStats.size(); i++) {
            assertEntityAndDtoEqual(expectedOrder.get(i), receivedStats.get(i));
        }
    }

    private static PlayerStats createPlayerStats(String name, int goals, int assists, int yellowCards, int redCards, Competition competition) {
        var playerStats = new PlayerStats(UUID.randomUUID(), UUID.randomUUID(), name);
        playerStats.setGoals(goals);
        playerStats.setAssists(assists);
        playerStats.setYellowCards(yellowCards);
        playerStats.setRedCards(redCards);
        playerStats.setCompetition(competition);
        return playerStats;
    }

    private static void assertEntityAndDtoEqual(PlayerStats entity, PlayerStatsDto dto) {
        assertEquals(entity.getPlayerId(), dto.getPlayerId());
        assertEquals(entity.getTeamId(), dto.getTeamId());
        assertEquals(entity.getName(), dto.getName());
        assertEquals(entity.getGoals(), dto.getGoals());
        assertEquals(entity.getAssists(), dto.getAssists());
        assertEquals(entity.getYellowCards(), dto.getYellowCards());
        assertEquals(entity.getRedCards(), dto.getRedCards());
    }
}
