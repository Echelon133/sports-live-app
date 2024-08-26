package ml.echelon133.matchservice.team.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.matchservice.match.model.ScoreInfoDto;
import ml.echelon133.matchservice.match.model.ShortTeamDto;
import ml.echelon133.matchservice.team.model.TeamDto;
import ml.echelon133.matchservice.team.model.TeamFormDetailsDto;
import ml.echelon133.matchservice.team.model.TeamFormDto;
import ml.echelon133.matchservice.coach.model.Coach;
import ml.echelon133.matchservice.coach.service.CoachService;
import ml.echelon133.matchservice.country.model.Country;
import ml.echelon133.matchservice.country.service.CountryService;
import ml.echelon133.matchservice.team.TestTeam;
import ml.echelon133.matchservice.team.TestTeamDto;
import ml.echelon133.matchservice.team.TestUpsertTeamDto;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.team.repository.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class TeamServiceTests {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private CountryService countryService;

    @Mock
    private CoachService coachService;

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
        var testDto = TestTeamDto.builder().build();
        var teamId = testDto.getId();

        // given
        given(teamRepository.findTeamById(teamId)).willReturn(Optional.of(testDto));

        // when
        TeamDto dto = teamService.findById(teamId);

        // then
        assertEquals(testDto, dto);
    }
    
    @Test
    @DisplayName("findEntityById throws when the repository does not store an entity with given id")
    public void findEntityById_EntityNotPresent_Throws() {
        var testId = UUID.randomUUID();

        // given
        given(teamRepository.findById(testId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamService.findEntityById(testId);
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", testId), message);
    }

    @Test
    @DisplayName("findEntityById throws when the repository stores an entity with given id but it's deleted")
    public void findEntityById_EntityPresentButDeleted_Throws() {
        var testId = UUID.randomUUID();
        var teamEntity = new Team();
        teamEntity.setDeleted(true);

        // given
        given(teamRepository.findById(testId)).willReturn(Optional.of(teamEntity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamService.findEntityById(testId);
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", testId), message);
    }

    @Test
    @DisplayName("findEntityById returns the entity when the repository stores it")
    public void findEntityById_EntityPresent_ReturnsEntity() throws ResourceNotFoundException {
        var testId = UUID.randomUUID();
        var teamEntity = new Team();

        // given
        given(teamRepository.findById(testId)).willReturn(Optional.of(teamEntity));

        // when
        var entity = teamService.findEntityById(testId);

        // then
        assertEquals(teamEntity, entity);
    }

    @Test
    @DisplayName("updateTeam throws when the team to update does not exist")
    public void updateTeam_TeamToUpdateEmpty_Throws() {
        var teamId = UUID.randomUUID();

        // given
        given(teamRepository.findById(teamId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamService.updateTeam(teamId, TestUpsertTeamDto.builder().build());
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", teamId), message);
    }

    @Test
    @DisplayName("updateTeam throws when the team to update is marked as deleted")
    public void updateTeam_TeamToUpdatePresentButMarkedAsDeleted_Throws() {
        var entity = TestTeam.builder().build();
        entity.setDeleted(true);
        var teamId = entity.getId();

        // given
        given(teamRepository.findById(teamId)).willReturn(Optional.of(entity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamService.updateTeam(teamId, TestUpsertTeamDto.builder().build());
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", teamId), message);
    }

    @Test
    @DisplayName("updateTeam throws when the country of the team to update does not exist")
    public void updateTeam_CountryEmpty_Throws() throws ResourceNotFoundException {
        var teamEntity = TestTeam.builder().build();
        var teamId = teamEntity.getId();
        var countryId = UUID.randomUUID();

        // given
        given(teamRepository.findById(teamId)).willReturn(Optional.of(teamEntity));
        given(countryService.findEntityById(countryId)).willThrow(
                new ResourceNotFoundException(Country.class, countryId)
        );

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamService.updateTeam(teamId, TestUpsertTeamDto.builder().countryId(countryId.toString()).build());
        }).getMessage();

        // then
        assertEquals(String.format("country %s could not be found", countryId), message);
    }

    @Test
    @DisplayName("updateTeam throws when the coach of the team to update does not exist")
    public void updateTeam_CoachEmpty_Throws() throws ResourceNotFoundException {
        var teamEntity = TestTeam.builder().build();
        var teamId = teamEntity.getId();
        var coachId = UUID.randomUUID();

        // given
        given(teamRepository.findById(teamId)).willReturn(Optional.of(teamEntity));
        given(countryService.findEntityById(any())).willReturn(new Country());
        given(coachService.findEntityById(coachId)).willThrow(
                new ResourceNotFoundException(Coach.class, coachId)
        );

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamService.updateTeam(teamId, TestUpsertTeamDto.builder().coachId(coachId.toString()).build());
        }).getMessage();

        // then
        assertEquals(String.format("coach %s could not be found", coachId), message);
    }

    @Test
    @DisplayName("updateTeam returns the expected dto after correctly updating a team")
    public void updateTeam_TeamUpdated_ReturnsDto() throws ResourceNotFoundException {
        var oldTeam = TestTeam.builder().build();
        var newCountry = new Country("Portugal", "PT");
        var newCountryId = newCountry.getId();
        var newCoach = new Coach("asdf123");
        var newCoachId = newCoach.getId();
        var newCrestUrl = "https://cdn.new.com/image.png";
        var updateDto = TestUpsertTeamDto
                .builder()
                .crestUrl(newCrestUrl)
                .countryId(newCountryId.toString())
                .coachId(newCoachId.toString())
                .build();
        var expectedTeam = TestTeam
                .builder()
                .id(oldTeam.getId())
                .name(updateDto.getName())
                .crestUrl(updateDto.getCrestUrl())
                .country(newCountry)
                .coach(newCoach)
                .build();

        // given
        given(teamRepository.findById(oldTeam.getId())).willReturn(Optional.of(oldTeam));
        given(countryService.findEntityById(newCountryId)).willReturn(newCountry);
        given(coachService.findEntityById(newCoachId)).willReturn(newCoach);
        given(teamRepository.save(argThat(t ->
                // Regular eq() only compares by entity's ID, which means that we need to use argThat()
                // if we want to make sure that the code actually tries to save a team with updated
                // values. Using eq() would make this test pass even if the method tried to save the
                // team without making any changes to it.
                t.getId().equals(oldTeam.getId()) &&
                        t.getName().equals(updateDto.getName()) &&
                        t.getCrestUrl().equals(updateDto.getCrestUrl()) &&
                        t.getCountry().getId().toString().equals(updateDto.getCountryId()) &&
                        t.getCoach().getId().toString().equals(updateDto.getCoachId())
        ))).willReturn(expectedTeam);

        // when
        var teamDto = teamService.updateTeam(oldTeam.getId(), updateDto);

        // then
        assertEquals(oldTeam.getId(), teamDto.getId());
        assertEquals(updateDto.getName(), teamDto.getName());
        assertEquals(updateDto.getCountryId(), teamDto.getCountry().getId().toString());
        assertEquals(updateDto.getCoachId(), teamDto.getCoach().getId().toString());
        assertEquals(updateDto.getCrestUrl(), teamDto.getCrestUrl());
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
        var expectedDto = TestTeamDto.builder().name(phrase).build();
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
    public void createTeam_CountryEmpty_Throws() throws ResourceNotFoundException {
        var countryId = UUID.randomUUID();

        // given
        given(countryService.findEntityById(countryId)).willThrow(
                new ResourceNotFoundException(Country.class, countryId)
        );

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamService.createTeam(TestUpsertTeamDto.builder().countryId(countryId.toString()).build());
        }).getMessage();

        // then
        assertEquals(String.format("country %s could not be found", countryId), message);
    }

    @Test
    @DisplayName("createTeam throws when the coach of the team does not exist")
    public void createTeam_CoachEmpty_Throws() throws ResourceNotFoundException {
        var coachId = UUID.randomUUID();

        // given
        given(coachService.findEntityById(coachId)).willThrow(
                new ResourceNotFoundException(Coach.class, coachId)
        );
        given(countryService.findEntityById(any())).willReturn(new Country());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamService.createTeam(TestUpsertTeamDto.builder().coachId(coachId.toString()).build());
        }).getMessage();

        // then
        assertEquals(String.format("coach %s could not be found", coachId), message);
    }

    @Test
    @DisplayName("createTeam returns the expected dto after correctly creating a team")
    public void createTeam_TeamCreated_ReturnsDto() throws ResourceNotFoundException {
        var country = new Country("Portugal", "PT");
        var coach = new Coach("Test");
        var createDto = TestUpsertTeamDto
                .builder()
                .countryId(country.getId().toString())
                .coachId(coach.getId().toString())
                .crestUrl("https://cdn.test.com/image.png")
                .build();
        var expectedTeam = TestTeam
                .builder()
                .name(createDto.getName())
                .crestUrl(createDto.getCrestUrl())
                .country(country)
                .coach(coach)
                .build();

        // given
        given(countryService.findEntityById(country.getId())).willReturn(country);
        given(coachService.findEntityById(coach.getId())).willReturn(coach);
        given(teamRepository.save(argThat(t ->
                // Regular eq() only compares by entity's ID, which means that we need to use argThat()
                // if we want to make sure that the code actually tries to save a team whose values
                // are taken from received upsert DTO
                t.getName().equals(createDto.getName()) &&
                        t.getCrestUrl().equals(createDto.getCrestUrl()) &&
                        t.getCountry().getId().toString().equals(createDto.getCountryId()) &&
                        t.getCoach().getId().toString().equals(createDto.getCoachId())
        ))).willReturn(expectedTeam);

        // when
        var teamDto = teamService.createTeam(createDto);

        // then
        assertEquals(expectedTeam.getId(), teamDto.getId());
        assertEquals(expectedTeam.getName(), teamDto.getName());
        assertEquals(expectedTeam.getCountry().getId(), teamDto.getCountry().getId());
        assertEquals(expectedTeam.getCoach().getId(), teamDto.getCoach().getId());
        assertEquals(expectedTeam.getCrestUrl(), teamDto.getCrestUrl());
    }

    private TeamFormDetailsDto createTestMatch(ShortTeamDto home, ShortTeamDto away, ScoreInfoDto score) {
        return TeamFormDetailsDto.from(UUID.randomUUID(), LocalDateTime.now(), home, away, score);
    }

    @Test
    @DisplayName("evaluateForm correctly evaluates the form from the perspective of a team")
    public void evaluateForm_MultipleVariedMatchResults_ReturnsExpectedForm() {
        var competitionId = UUID.randomUUID();

        // teamA
        var teamAEntity = TestTeam.builder().build();
        var teamAId = teamAEntity.getId();
        var teamA = ShortTeamDto.from(teamAId, teamAEntity.getName(), teamAEntity.getCrestUrl());

        // teamB
        var teamBEntity = TestTeam.builder().build();
        var teamBId = teamBEntity.getId();
        var teamB = ShortTeamDto.from(teamBId, teamBEntity.getName(), teamBEntity.getCrestUrl());

        List<TeamFormDetailsDto> formEval = List.of(
                //      * teamA vs teamB (3:2 - teamA wins)
                createTestMatch(teamA, teamB, ScoreInfoDto.from(3, 2)),
                //      * teamA vs teamB (2:2 - draw)
                createTestMatch(teamA, teamB, ScoreInfoDto.from(2, 2)),
                //      * teamB vs teamA (4:4 - draw)
                createTestMatch(teamB, teamA, ScoreInfoDto.from(4, 4)),
                //      * teamA vs teamB (1:4 - teamB wins)
                createTestMatch(teamA, teamB, ScoreInfoDto.from(1, 4)),
                //      * teamB vs teamA (2:0 - teamB wins)
                createTestMatch(teamB, teamA, ScoreInfoDto.from(2, 0))
        );

        // given
        given(teamRepository.findFormEvaluationMatches(teamAId, competitionId)).willReturn(formEval);
        given(teamRepository.findFormEvaluationMatches(teamBId, competitionId)).willReturn(formEval);

        // when
        var formOfTeamA = teamService.evaluateForm(teamAId, competitionId)
                .stream().map(TeamFormDto::getForm).collect(Collectors.toList());
        var formOfTeamB = teamService.evaluateForm(teamBId, competitionId)
                .stream().map(TeamFormDto::getForm).collect(Collectors.toList());

        // then
        // from the teamA's perspective, their form is WDDLL
        assertEquals(List.of('W', 'D', 'D', 'L', 'L'), formOfTeamA);
        // from the teamB's perspective, their form is LDDWW
        assertEquals(List.of('L', 'D', 'D', 'W', 'W'), formOfTeamB);
    }

    @Test
    @DisplayName("evaluateGeneralForm correctly evaluates the form from the perspective of a team")
    public void evaluateGeneralForm_MultipleVariedMatchResults_ReturnsExpectedForm() {
        // teamA
        var teamAEntity = TestTeam.builder().build();
        var teamAId = teamAEntity.getId();
        var teamA = ShortTeamDto.from(teamAId, teamAEntity.getName(), teamAEntity.getCrestUrl());

        // teamB
        var teamBEntity = TestTeam.builder().build();
        var teamBId = teamBEntity.getId();
        var teamB = ShortTeamDto.from(teamBId, teamBEntity.getName(), teamBEntity.getCrestUrl());

        List<TeamFormDetailsDto> formEval = List.of(
                //      * teamA vs teamB (3:2 - teamA wins)
                createTestMatch(teamA, teamB, ScoreInfoDto.from(3, 2)),
                //      * teamA vs teamB (2:2 - draw)
                createTestMatch(teamA, teamB, ScoreInfoDto.from(2, 2)),
                //      * teamB vs teamA (4:4 - draw)
                createTestMatch(teamB, teamA, ScoreInfoDto.from(4, 4)),
                //      * teamA vs teamB (1:4 - teamB wins)
                createTestMatch(teamA, teamB, ScoreInfoDto.from(1, 4)),
                //      * teamB vs teamA (2:0 - teamB wins)
                createTestMatch(teamB, teamA, ScoreInfoDto.from(2, 0))
        );

        // given
        given(teamRepository.findGeneralFormEvaluationMatches(teamAId)).willReturn(formEval);
        given(teamRepository.findGeneralFormEvaluationMatches(teamBId)).willReturn(formEval);

        // when
        var formOfTeamA = teamService.evaluateGeneralForm(teamAId)
                .stream().map(TeamFormDto::getForm).collect(Collectors.toList());
        var formOfTeamB = teamService.evaluateGeneralForm(teamBId)
                .stream().map(TeamFormDto::getForm).collect(Collectors.toList());

        // then
        // from the teamA's perspective, their form is WDDLL
        assertEquals(List.of('W', 'D', 'D', 'L', 'L'), formOfTeamA);
        // from the teamB's perspective, their form is LDDWW
        assertEquals(List.of('L', 'D', 'D', 'W', 'W'), formOfTeamB);
    }
}
