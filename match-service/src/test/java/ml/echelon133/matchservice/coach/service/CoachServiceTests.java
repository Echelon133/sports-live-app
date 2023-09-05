package ml.echelon133.matchservice.coach.service;

import ml.echelon133.common.coach.dto.CoachDto;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.matchservice.coach.model.Coach;
import ml.echelon133.matchservice.coach.model.UpsertCoachDto;
import ml.echelon133.matchservice.coach.repository.CoachRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CoachServiceTests {

    @Mock
    private CoachRepository coachRepository;

    @InjectMocks
    private CoachService coachService;


    @Test
    @DisplayName("findById throws when there is no entity in the repository")
    public void findById_EntityNotPresent_Throws() {
        var testId = UUID.randomUUID();

        // given
        given(coachRepository.findCoachById(testId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            coachService.findById(testId);
        }).getMessage();

        // then
        assertEquals(String.format("coach %s could not be found", testId), message);
    }

    @Test
    @DisplayName("findById returns the dto when the coach is present")
    public void findById_EntityPresent_ReturnsDto() throws ResourceNotFoundException {
        var testDto = CoachDto.from(UUID.randomUUID(), "Test");
        var testId = testDto.getId();

        // given
        given(coachRepository.findCoachById(testId)).willReturn(Optional.of(testDto));

        // when
        CoachDto dto = coachService.findById(testId);

        // then
        assertEquals(testDto, dto);
    }

    @Test
    @DisplayName("createCoach calls repository's save and returns correct dto")
    public void createCoach_ValidDto_CorrectlySavesAndReturns() {
        var idToSave = UUID.randomUUID();
        var initialDto = new UpsertCoachDto("Test");
        var entity = new Coach(initialDto.getName());
        entity.setId(idToSave);

        // given
        given(coachRepository.save(argThat(v -> v.getName().equals(initialDto.getName())))).willReturn(entity);

        // when
        CoachDto savedDto = coachService.createCoach(initialDto);

        // then
        verify(coachRepository, times(1)).save(any());
        assertEquals(entity.getId(), savedDto.getId());
        assertEquals(entity.getName(), savedDto.getName());
    }

    @Test
    @DisplayName("markCoachAsDeleted calls repository's method and returns correct number of entries marked as deleted")
    public void markCoachAsDeleted_ProvidedId_CorrectlyCallsMethodAndReturnsCount() {
        var idToDelete = UUID.randomUUID();

        // given
        given(coachRepository.markCoachAsDeleted(idToDelete)).willReturn(1);

        // when
        Integer countDeleted = coachService.markCoachAsDeleted(idToDelete);

        // then
        assertEquals(1, countDeleted);
    }

    @Test
    @DisplayName("updateCoach throws when there is no entity in the repository")
    public void updateCoach_EntityNotPresent_Throws() {
        var testId = UUID.randomUUID();

        //
        // given
        given(coachRepository.findById(testId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            coachService.updateCoach(testId, new UpsertCoachDto("Test"));
        }).getMessage();

        // then
        assertEquals(String.format("coach %s could not be found", testId), message);
    }

    @Test
    @DisplayName("updateCoach throws when there is no entity in the repository because it's been deleted")
    public void updateCoach_EntityMarkedAsDeleted_Throws() {
        var testId = UUID.randomUUID();
        var entity = new Coach("Test");
        entity.setDeleted(true);

        // given
        given(coachRepository.findById(testId)).willReturn(Optional.of(entity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            coachService.updateCoach(testId, new UpsertCoachDto("Test"));
        }).getMessage();

        // then
        assertEquals(String.format("coach %s could not be found", testId), message);
    }


    @Test
    @DisplayName("updateCoach changes the fields of the entity")
    public void updateCoach_EntityPresent_UpdatesFieldValuesOfEntity() throws ResourceNotFoundException {
        var originalEntity = new Coach("Test");
        var newName = "Asdf";
        var expectedUpdatedEntity = new Coach(newName);
        expectedUpdatedEntity.setId(originalEntity.getId());

        // given
        given(coachRepository.findById(originalEntity.getId())).willReturn(Optional.of(originalEntity));
        given(coachRepository.save(argThat(v -> v.getName().equals(newName)))).willReturn(expectedUpdatedEntity);

        // when
        CoachDto updated = coachService.updateCoach(originalEntity.getId(), new UpsertCoachDto(newName));

        // then
        assertEquals(newName, updated.getName());
    }

    @Test
    @DisplayName("findCoachesByName correctly calls the repository method")
    public void findCoachesByName_CustomPhraseAndPageable_CorrectlyCallsRepository() {
        var phrase = "test";
        var pageable = Pageable.ofSize(7).withPage(4);
        var expectedDto = CoachDto.from(UUID.randomUUID(), "test");
        var expectedPage = new PageImpl<>(List.of(expectedDto), pageable, 1);

        // given
        given(coachRepository.findAllByNameContaining(
                eq(phrase),
                argThat(p -> p.getPageSize() == 7 && p.getPageNumber() == 4)
        )).willReturn(expectedPage);

        // when
        var result = coachService.findCoachesByName(phrase, pageable);

        // then
        assertEquals(1, result.getNumberOfElements());
    }
}
