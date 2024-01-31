package pl.echelon133.competitionservice.competition.repository;

import ml.echelon133.common.competition.dto.CompetitionDto;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Disable kubernetes during tests
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@DataJpaTest
public class CompetitionRepositoryTests {

    private final CompetitionRepository competitionRepository;

    @Autowired
    public CompetitionRepositoryTests(CompetitionRepository competitionRepository) {
        this.competitionRepository = competitionRepository;
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
}
