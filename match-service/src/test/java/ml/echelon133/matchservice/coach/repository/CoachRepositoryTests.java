package ml.echelon133.matchservice.coach.repository;

import ml.echelon133.common.coach.dto.CoachDto;
import ml.echelon133.matchservice.coach.model.Coach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Disable kubernetes during tests
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@DataJpaTest
public class CoachRepositoryTests {

    @Autowired
    private CoachRepository coachRepository;

    @Test
    @DisplayName("findCoachById native query finds empty when the coach does not exist")
    public void findCoachById_CoachDoesNotExist_IsEmpty() {
        var id = UUID.randomUUID();

        // when
        Optional<CoachDto> coachDto = coachRepository.findCoachById(id);

        // then
        assertTrue(coachDto.isEmpty());
    }

    @Test
    @DisplayName("findCoachById native query finds coach when the coach exists")
    public void findCoachById_CoachExists_IsPresent() {
        var saved = coachRepository.save(new Coach("Pep Guardiola"));

        // when
        Optional<CoachDto> coachDto = coachRepository.findCoachById(saved.getId());

        // then
        assertTrue(coachDto.isPresent());
        var result = coachDto.get();
        assertEquals(result.getName(), saved.getName());
    }

    @Test
    @DisplayName("findCoachById native query does not fetch coaches marked as deleted")
    public void findCoachById_CoachMarkedAsDeleted_IsEmpty() {
        var coachToSave = new Coach("Jurgen Klopp");
        coachToSave.setDeleted(true);
        var saved = coachRepository.saveAndFlush(coachToSave);

        // when
        Optional<CoachDto> coachDto = coachRepository.findCoachById(saved.getId());

        // then
        assertTrue(coachDto.isEmpty());
    }

    @Test
    @DisplayName("markCoachAsDeleted native query only affects the coach with specified id")
    public void markCoachAsDeleted_SpecifiedCoachId_OnlyMarksSpecifiedCoach() {
        var coach0 = new Coach("Antonio Conte");
        var coach1 = new Coach("Pep Guardiola");
        var coach2 = new Coach("Zinedine Zidane");

        var saved0 = coachRepository.save(coach0);
        coachRepository.save(coach1);
        coachRepository.save(coach2);

        // when
        Integer countDeleted = coachRepository.markCoachAsDeleted(saved0.getId());
        // findCoachById filters out `deleted` entities
        Optional<CoachDto> coach = coachRepository.findCoachById(saved0.getId());

        // then
        assertEquals(1, countDeleted);
        assertTrue(coach.isEmpty());
    }

    @Test
    @DisplayName("markCoachAsDeleted native query only affects not deleted coaches")
    public void markCoachAsDeleted_CoachAlreadyMarkedAsDeleted_IsNotTouchedByQuery() {
        var coach0 = new Coach("Test");
        coach0.setDeleted(true); // make deleted by default
        var saved0 = coachRepository.save(coach0);

        // when
        Integer countDeleted = coachRepository.markCoachAsDeleted(saved0.getId());

        // then
        assertEquals(0, countDeleted);
    }

    @Test
    @DisplayName("findAllByNameContaining native query only finds results which contain the specified phrase")
    public void findAllByNameContaining_MultipleCoaches_OnlyFindsMatchingCoaches() {
        coachRepository.save(new Coach("Carlo Ancellotti"));
        coachRepository.save(new Coach("Pep Guardiola"));
        coachRepository.save(new Coach("Hansi Flick"));

        // when
        Page<CoachDto> result = coachRepository.findAllByNameContaining("o", Pageable.ofSize(10));

        // then
        assertEquals(2, result.getTotalElements());
        List<String> names = result.getContent().stream().map(CoachDto::getName).collect(Collectors.toList());
        assertTrue(names.contains("Carlo Ancellotti"));
        assertTrue(names.contains("Pep Guardiola"));
    }

    @Test
    @DisplayName("findAllByNameContaining native query is case-insensitive")
    public void findAllByNameContaining_MultipleCoaches_SearchIsCaseInsensitive() {
        coachRepository.save(new Coach("Carlo Ancellotti"));
        coachRepository.save(new Coach("Pep Guardiola"));
        coachRepository.save(new Coach("Hansi Flick"));

        // when
        Page<CoachDto> result = coachRepository.findAllByNameContaining("an", Pageable.ofSize(10));

        // then
        assertEquals(2, result.getTotalElements());
        List<String> names = result.getContent().stream().map(CoachDto::getName).collect(Collectors.toList());
        assertTrue(names.contains("Hansi Flick"));
        assertTrue(names.contains("Carlo Ancellotti"));
    }

    @Test
    @DisplayName("findAllByNameContaining native query only finds non-deleted results which contain the specified phrase")
    public void findAllByNameContaining_SomeDeletedCoaches_OnlyFindsMatchingNonDeletedCoaches() {
        var deletedCoach = new Coach("Carlo Ancellotti");
        deletedCoach.setDeleted(true);
        coachRepository.save(deletedCoach);
        coachRepository.save(new Coach("Pep Guardiola"));
        coachRepository.save(new Coach("Hansi Flick"));

        // when
        Page<CoachDto> result = coachRepository.findAllByNameContaining("o", Pageable.ofSize(10));

        // then
        assertEquals(1, result.getTotalElements());
        List<String> names = result.getContent().stream().map(CoachDto::getName).collect(Collectors.toList());
        assertTrue(names.contains("Pep Guardiola"));
    }

    @Test
    @DisplayName("findAllByNameContaining native query takes page size information from Pageable into account")
    public void findAllByNameContaining_PageableCustomPageSize_UsesPageableInfo() {
        // when
        Page<CoachDto> result = coachRepository.findAllByNameContaining("test", Pageable.ofSize(8));

        // then
        assertEquals(8, result.getPageable().getPageSize());
    }

    @Test
    @DisplayName("findAllByNameContaining native query takes page number information from Pageable into account")
    public void findAllByNameContaining_PageableCustomPageNumber_UsesPageableInfo() {
        var pageable = PageRequest.ofSize(6).withPage(5);

        // when
        Page<CoachDto> result = coachRepository.findAllByNameContaining("test", pageable);

        // then
        var receivedPageable = result.getPageable();
        assertEquals(6, receivedPageable.getPageSize());
        assertEquals(5, receivedPageable.getPageNumber());
    }
}
