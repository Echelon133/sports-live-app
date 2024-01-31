package pl.echelon133.competitionservice.competition.repository;

import ml.echelon133.common.competition.dto.CompetitionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
}
