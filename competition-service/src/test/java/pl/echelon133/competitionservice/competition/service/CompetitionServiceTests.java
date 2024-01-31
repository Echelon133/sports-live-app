package pl.echelon133.competitionservice.competition.service;

import ml.echelon133.common.competition.dto.CompetitionDto;
import ml.echelon133.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.echelon133.competitionservice.competition.repository.CompetitionRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class CompetitionServiceTests {

    @Mock
    private CompetitionRepository competitionRepository;

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
}
