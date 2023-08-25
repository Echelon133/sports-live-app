package ml.echelon133.matchservice.referee.repository;

import ml.echelon133.common.referee.dto.RefereeDto;
import ml.echelon133.matchservice.referee.model.Referee;
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
public class RefereeRepositoryTests {

    @Autowired
    private RefereeRepository refereeRepository;

    @Test
    @DisplayName("findRefereeById native query finds empty when the referee does not exist")
    public void findRefereeById_RefereeDoesNotExist_IsEmpty() {
        var id = UUID.randomUUID();

        // when
        Optional<RefereeDto> refereeDto = refereeRepository.findRefereeById(id);

        // then
        assertTrue(refereeDto.isEmpty());
    }

    @Test
    @DisplayName("findRefereeById native query finds referee when the referee exists")
    public void findRefereeById_RefereeExists_IsPresent() {
        var saved = refereeRepository.save(new Referee("Szymon Marciniak"));

        // when
        Optional<RefereeDto> refereeDto = refereeRepository.findRefereeById(saved.getId());

        // then
        assertTrue(refereeDto.isPresent());
        var result = refereeDto.get();
        assertEquals(result.getName(), saved.getName());
    }

    @Test
    @DisplayName("findRefereeById native query does not fetch referees marked as deleted")
    public void findRefereeById_RefereeMarkedAsDeleted_IsEmpty() {
        var refereeToSave = new Referee("Howard Webb");
        refereeToSave.setDeleted(true);
        var saved = refereeRepository.saveAndFlush(refereeToSave);

        // when
        Optional<RefereeDto> refereeDto = refereeRepository.findRefereeById(saved.getId());

        // then
        assertTrue(refereeDto.isEmpty());
    }

    @Test
    @DisplayName("markRefereeAsDeleted native query only affects the referee with specified id")
    public void markRefereeAsDeleted_SpecifiedRefereeId_OnlyMarksSpecifiedReferee() {
        var referee0 = new Referee("Szymon Marciniak");
        var referee1 = new Referee("Howard Webb");
        var referee2 = new Referee("Michael Oliver");

        var saved0 = refereeRepository.save(referee0);
        refereeRepository.save(referee1);
        refereeRepository.save(referee2);

        // when
        Integer countDeleted = refereeRepository.markRefereeAsDeleted(saved0.getId());
        // findRefereeById filters out `deleted` entities
        Optional<RefereeDto> referee = refereeRepository.findRefereeById(saved0.getId());

        // then
        assertEquals(1, countDeleted);
        assertTrue(referee.isEmpty());
    }

    @Test
    @DisplayName("markRefereeAsDeleted native query only affects not deleted referees")
    public void markRefereeAsDeleted_RefereeAlreadyMarkedAsDeleted_IsNotTouchedByQuery() {
        var referee0 = new Referee("Test");
        referee0.setDeleted(true); // make deleted by default
        var saved0 = refereeRepository.save(referee0);

        // when
        Integer countDeleted = refereeRepository.markRefereeAsDeleted(saved0.getId());

        // then
        assertEquals(0, countDeleted);
    }

    @Test
    @DisplayName("findAllByNameContaining native query only finds results which contain the specified phrase")
    public void findAllByNameContaining_MultipleReferees_OnlyFindsMatchingReferees() {
        refereeRepository.save(new Referee("Michael Oliver"));
        refereeRepository.save(new Referee("Michael Salisbury"));
        refereeRepository.save(new Referee("Anthony Tailor"));

        // when
        Page<RefereeDto> result = refereeRepository.findAllByNameContaining("Michael", Pageable.ofSize(10));

        // then
        assertEquals(2, result.getTotalElements());
        List<String> names = result.getContent().stream().map(RefereeDto::getName).collect(Collectors.toList());
        assertTrue(names.contains("Michael Oliver"));
        assertTrue(names.contains("Michael Salisbury"));
    }

    @Test
    @DisplayName("findAllByNameContaining native query is case-insensitive")
    public void findAllByNameContaining_MultipleReferees_SearchIsCaseInsensitive() {
        refereeRepository.save(new Referee("Michael Oliver"));
        refereeRepository.save(new Referee("Michael Salisbury"));
        refereeRepository.save(new Referee("Anthony Tailor"));

        // when
        Page<RefereeDto> result = refereeRepository.findAllByNameContaining("miCHaEl", Pageable.ofSize(10));

        // then
        assertEquals(2, result.getTotalElements());
        List<String> names = result.getContent().stream().map(RefereeDto::getName).collect(Collectors.toList());
        assertTrue(names.contains("Michael Oliver"));
        assertTrue(names.contains("Michael Salisbury"));
    }

    @Test
    @DisplayName("findAllByNameContaining native query only finds non-deleted results which contain the specified phrase")
    public void findAllByNameContaining_SomeDeletedReferees_OnlyFindsMatchingNonDeletedReferees() {
        var deletedReferee = new Referee("Michael Oliver");
        deletedReferee.setDeleted(true);
        refereeRepository.save(deletedReferee);
        refereeRepository.save(new Referee("Michael Salisbury"));
        refereeRepository.save(new Referee("Anthony Tailor"));

        // when
        Page<RefereeDto> result = refereeRepository.findAllByNameContaining("Michael", Pageable.ofSize(10));

        // then
        assertEquals(1, result.getTotalElements());
        List<String> names = result.getContent().stream().map(RefereeDto::getName).collect(Collectors.toList());
        assertTrue(names.contains("Michael Salisbury"));
    }

    @Test
    @DisplayName("findAllByNameContaining native query takes page size information from Pageable into account")
    public void findAllByNameContaining_PageableCustomPageSize_UsesPageableInfo() {
        // when
        Page<RefereeDto> result = refereeRepository.findAllByNameContaining("Nou", Pageable.ofSize(8));

        // then
        assertEquals(8, result.getPageable().getPageSize());
    }

    @Test
    @DisplayName("findAllByNameContaining native query takes page number information from Pageable into account")
    public void findAllByNameContaining_PageableCustomPageNumber_UsesPageableInfo() {
        var pageable = PageRequest.ofSize(6).withPage(5);

        // when
        Page<RefereeDto> result = refereeRepository.findAllByNameContaining("Nou", pageable);

        // then
        var receivedPageable = result.getPageable();
        assertEquals(6, receivedPageable.getPageSize());
        assertEquals(5, receivedPageable.getPageNumber());
    }
}
