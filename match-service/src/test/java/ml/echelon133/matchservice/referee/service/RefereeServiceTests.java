package ml.echelon133.matchservice.referee.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.referee.dto.RefereeDto;
import ml.echelon133.matchservice.referee.model.Referee;
import ml.echelon133.matchservice.referee.model.UpsertRefereeDto;
import ml.echelon133.matchservice.referee.repository.RefereeRepository;
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
public class RefereeServiceTests {

    @Mock
    private RefereeRepository refereeRepository;

    @InjectMocks
    private RefereeService refereeService;


    @Test
    @DisplayName("findById throws when there is no entity in the repository")
    public void findById_EntityNotPresent_Throws() {
        var testId = UUID.randomUUID();

        // given
        given(refereeRepository.findRefereeById(testId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            refereeService.findById(testId);
        }).getMessage();

        // then
        assertEquals(String.format("referee %s could not be found", testId), message);
    }

    @Test
    @DisplayName("findById returns the dto when the referee is present")
    public void findById_EntityPresent_ReturnsDto() throws ResourceNotFoundException {
        var testDto = RefereeDto.from(UUID.randomUUID(), "Test");
        var testId = testDto.getId();

        // given
        given(refereeRepository.findRefereeById(testId)).willReturn(Optional.of(testDto));

        // when
        RefereeDto dto = refereeService.findById(testId);

        // then
        assertEquals(testDto, dto);
    }

    @Test
    @DisplayName("createReferee calls repository's save and returns correct dto")
    public void createReferee_ValidDto_CorrectlySavesAndReturns() {
        var idToSave = UUID.randomUUID();
        var initialDto = new UpsertRefereeDto("Test");
        var entity = new Referee(initialDto.getName());
        entity.setId(idToSave);

        // given
        given(refereeRepository.save(argThat(v -> v.getName().equals(initialDto.getName())))).willReturn(entity);

        // when
        RefereeDto savedDto = refereeService.createReferee(initialDto);

        // then
        verify(refereeRepository, times(1)).save(any());
        assertEquals(entity.getId(), savedDto.getId());
        assertEquals(entity.getName(), savedDto.getName());
    }

    @Test
    @DisplayName("markRefereeAsDeleted calls repository's method and returns correct number of entries marked as deleted")
    public void markRefereeAsDeleted_ProvidedId_CorrectlyCallsMethodAndReturnsCount() {
        var idToDelete = UUID.randomUUID();

        // given
        given(refereeRepository.markRefereeAsDeleted(idToDelete)).willReturn(1);

        // when
        Integer countDeleted = refereeService.markRefereeAsDeleted(idToDelete);

        // then
        assertEquals(1, countDeleted);
    }

    @Test
    @DisplayName("updateReferee throws when there is no entity in the repository")
    public void updateReferee_EntityNotPresent_Throws() {
        var testId = UUID.randomUUID();

        //
        // given
        given(refereeRepository.findById(testId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            refereeService.updateReferee(testId, new UpsertRefereeDto("Test"));
        }).getMessage();

        // then
        assertEquals(String.format("referee %s could not be found", testId), message);
    }

    @Test
    @DisplayName("updateReferee throws when there is no entity in the repository because it's been deleted")
    public void updateReferee_EntityMarkedAsDeleted_Throws() {
        var testId = UUID.randomUUID();
        var entity = new Referee("Test");
        entity.setDeleted(true);

        // given
        given(refereeRepository.findById(testId)).willReturn(Optional.of(entity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            refereeService.updateReferee(testId, new UpsertRefereeDto("Test"));
        }).getMessage();

        // then
        assertEquals(String.format("referee %s could not be found", testId), message);
    }


    @Test
    @DisplayName("updateReferee changes the fields of the entity")
    public void updateReferee_EntityPresent_UpdatesFieldValuesOfEntity() throws ResourceNotFoundException {
        var originalEntity = new Referee("Test");
        var newName = "Asdf";
        var expectedUpdatedEntity = new Referee(newName);
        expectedUpdatedEntity.setId(originalEntity.getId());

        // given
        given(refereeRepository.findById(originalEntity.getId())).willReturn(Optional.of(originalEntity));
        given(refereeRepository.save(argThat(v -> v.getName().equals(newName)))).willReturn(expectedUpdatedEntity);

        // when
        RefereeDto updated = refereeService.updateReferee(originalEntity.getId(), new UpsertRefereeDto(newName));

        // then
        assertEquals(newName, updated.getName());
    }

    @Test
    @DisplayName("findRefereesByName correctly calls the repository method")
    public void findRefereesByName_CustomPhraseAndPageable_CorrectlyCallsRepository() {
        var phrase = "test";
        var pageable = Pageable.ofSize(7).withPage(4);
        var expectedDto = RefereeDto.from(UUID.randomUUID(), "test");
        var expectedPage = new PageImpl<>(List.of(expectedDto), pageable, 1);

        // given
        given(refereeRepository.findAllByNameContaining(
                eq(phrase),
                argThat(p -> p.getPageSize() == 7 && p.getPageNumber() == 4)
        )).willReturn(expectedPage);

        // when
        var result = refereeService.findRefereesByName(phrase, pageable);

        // then
        assertEquals(1, result.getNumberOfElements());
    }
}
