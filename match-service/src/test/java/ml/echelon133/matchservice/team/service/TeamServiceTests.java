package ml.echelon133.matchservice.team.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.team.dto.TeamDto;
import ml.echelon133.matchservice.country.model.Country;
import ml.echelon133.matchservice.country.repository.CountryRepository;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.team.model.UpsertTeamDto;
import ml.echelon133.matchservice.team.repository.TeamRepository;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class TeamServiceTests {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private TeamService teamService;


    @Test
    @DisplayName("findById throws when there is no entity in the repository")
    public void findById_EntityNotPresent_Throws() {
        var teamId = UUID.randomUUID();

        // given
        given(teamRepository.findTeamById(teamId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamService.findById(teamId);
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", teamId), message);
    }

    @Test
    @DisplayName("findById returns the dto when the team is present")
    public void findById_EntityPresent_ReturnsDto() throws ResourceNotFoundException {
        var testDto = TeamDto.builder().build();
        var teamId = testDto.getId();

        // given
        given(teamRepository.findTeamById(teamId)).willReturn(Optional.of(testDto));

        // when
        TeamDto dto = teamService.findById(teamId);

        // then
        assertEquals(testDto, dto);
    }

    @Test
    @DisplayName("updateTeam throws when the team to update does not exist")
    public void updateTeam_TeamToUpdateEmpty_Throws() {
        var teamId = UUID.randomUUID();

        // given
        given(teamRepository.findById(teamId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamService.updateTeam(teamId, new UpsertTeamDto());
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", teamId), message);
    }

    @Test
    @DisplayName("updateTeam throws when the team to update is marked as deleted")
    public void updateTeam_TeamToUpdatePresentButMarkedAsDeleted_Throws() {
        var entity = new Team();
        entity.setDeleted(true);
        var teamId = entity.getId();

        // given
        given(teamRepository.findById(teamId)).willReturn(Optional.of(entity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamService.updateTeam(teamId, new UpsertTeamDto());
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", teamId), message);
    }

    @Test
    @DisplayName("updateTeam throws when the country of the team to update does not exist")
    public void updateTeam_CountryEmpty_Throws() {
        var teamEntity = new Team();
        var teamId = teamEntity.getId();
        var countryId = UUID.randomUUID();

        // given
        given(teamRepository.findById(teamId)).willReturn(Optional.of(teamEntity));
        given(countryRepository.findById(countryId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamService.updateTeam(teamId, new UpsertTeamDto("test", countryId.toString()));
        }).getMessage();

        // then
        assertEquals(String.format("country %s could not be found", countryId), message);
    }

    @Test
    @DisplayName("updateTeam throws when the country of the team to update is marked as deleted")
    public void updateTeam_CountryPresentButMarkedAsDeleted_Throws() {
        var teamEntity = new Team();
        var teamId = teamEntity.getId();
        var countryEntity = new Country();
        var countryId = countryEntity.getId();
        countryEntity.setDeleted(true);

        // given
        given(teamRepository.findById(teamId)).willReturn(Optional.of(teamEntity));
        given(countryRepository.findById(countryId)).willReturn(Optional.of(countryEntity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamService.updateTeam(teamId, new UpsertTeamDto("test", countryId.toString()));
        }).getMessage();

        // then
        assertEquals(String.format("country %s could not be found", countryId), message);
    }

    @Test
    @DisplayName("updateTeam returns the expected dto after correctly updating a team")
    public void updateTeam_TeamUpdated_ReturnsDto() throws ResourceNotFoundException {
        var oldTeam = new Team("Test", new Country("Poland", "PL"));
        var newCountry = new Country("Portugal", "PT");
        var newCountryId = newCountry.getId();
        var updateDto = new UpsertTeamDto("new team name", newCountryId.toString());
        var expectedTeam = new Team(
                updateDto.getName(),
                newCountry
        );
        expectedTeam.setId(oldTeam.getId());

        // given
        given(teamRepository.findById(oldTeam.getId())).willReturn(Optional.of(oldTeam));
        given(countryRepository.findById(newCountryId)).willReturn(Optional.of(newCountry));
        given(teamRepository.save(argThat(p ->
                // Regular eq() only compares by entity's ID, which means that we need to use argThat()
                // if we want to make sure that the code actually tries to save a team with updated
                // values. Using eq() would make this test pass even if the method tried to save the
                // team without making any changes to it.
                p.getId().equals(oldTeam.getId()) &&
                        p.getName().equals(updateDto.getName()) &&
                        p.getCountry().getId().toString().equals(updateDto.getCountryId())
        ))).willReturn(expectedTeam);

        // when
        var teamDto = teamService.updateTeam(oldTeam.getId(), updateDto);

        // then
        assertEquals(oldTeam.getId(), teamDto.getId());
        assertEquals(updateDto.getName(), teamDto.getName());
        assertEquals(updateDto.getCountryId(), teamDto.getCountry().getId().toString());
    }

    @Test
    @DisplayName("markTeamAsDeleted calls repository's method and returns correct number of entries marked as deleted")
    public void markTeamAsDeleted_ProvidedId_CorrectlyCallsMethodAndReturnsCount() {
        var idToDelete = UUID.randomUUID();

        // given
        given(teamRepository.markTeamAsDeleted(idToDelete)).willReturn(1);

        // when
        Integer countDeleted = teamService.markTeamAsDeleted(idToDelete);

        // then
        assertEquals(1, countDeleted);
    }

    @Test
    @DisplayName("findTeamsByName correctly calls the repository method")
    public void findTeamsByName_CustomPhraseAndPageable_CorrectlyCallsRepository() {
        var phrase = "test";
        var pageable = Pageable.ofSize(7).withPage(4);
        var expectedDto = TeamDto.builder().name(phrase).build();
        var expectedPage = new PageImpl<>(List.of(expectedDto), pageable, 1);

        // given
        given(teamRepository.findAllByNameContaining(
                eq(phrase),
                argThat(p -> p.getPageSize() == 7 && p.getPageNumber() == 4)
        )).willReturn(expectedPage);

        // when
        var result = teamService.findTeamsByName(phrase, pageable);

        // then
        assertEquals(1, result.getNumberOfElements());
    }

    @Test
    @DisplayName("createTeam throws when the country of the team does not exist")
    public void createTeam_CountryEmpty_Throws() {
        var countryId = UUID.randomUUID();

        // given
        given(countryRepository.findById(countryId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamService.createTeam(new UpsertTeamDto("test", countryId.toString()));
        }).getMessage();

        // then
        assertEquals(String.format("country %s could not be found", countryId), message);
    }

    @Test
    @DisplayName("createTeam throws when the country of the team is marked as deleted")
    public void createTeam_CountryPresentButMarkedAsDeleted_Throws() {
        var countryEntity = new Country();
        var countryId = countryEntity.getId();
        countryEntity.setDeleted(true);

        // given
        given(countryRepository.findById(countryId)).willReturn(Optional.of(countryEntity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamService.createTeam(new UpsertTeamDto("test", countryId.toString()));
        }).getMessage();

        // then
        assertEquals(String.format("country %s could not be found", countryId), message);
    }

    @Test
    @DisplayName("createTeam returns the expected dto after correctly creating a team")
    public void createTeam_TeamCreated_ReturnsDto() throws ResourceNotFoundException {
        var country = new Country("Portugal", "PT");
        var createDto = new UpsertTeamDto("some name", country.getId().toString());
        var expectedTeam = new Team(
                createDto.getName(),
                country
        );

        // given
        given(countryRepository.findById(country.getId())).willReturn(Optional.of(country));
        given(teamRepository.save(argThat(p ->
                // Regular eq() only compares by entity's ID, which means that we need to use argThat()
                // if we want to make sure that the code actually tries to save a team whose values
                // are taken from received upsert DTO
                p.getName().equals(createDto.getName()) &&
                        p.getCountry().getId().toString().equals(createDto.getCountryId())
        ))).willReturn(expectedTeam);

        // when
        var teamDto = teamService.createTeam(createDto);

        // then
        assertEquals(expectedTeam.getId(), teamDto.getId());
        assertEquals(expectedTeam.getName(), teamDto.getName());
        assertEquals(expectedTeam.getCountry().getId(), teamDto.getCountry().getId());
    }
}
