package ml.echelon133.matchservice.team.controller;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.matchservice.MatchServiceApplication;
import ml.echelon133.matchservice.TestValidatorFactory;
import ml.echelon133.matchservice.coach.constraints.CoachExists;
import ml.echelon133.matchservice.coach.repository.CoachRepository;
import ml.echelon133.matchservice.match.model.*;
import ml.echelon133.matchservice.match.service.MatchService;
import ml.echelon133.matchservice.player.constraints.PlayerExists;
import ml.echelon133.matchservice.player.repository.PlayerRepository;
import ml.echelon133.matchservice.team.TestTeamDto;
import ml.echelon133.matchservice.team.TestTeamPlayerDto;
import ml.echelon133.matchservice.team.TestUpsertTeamDto;
import ml.echelon133.matchservice.team.exception.NumberAlreadyTakenException;
import ml.echelon133.matchservice.team.model.*;
import ml.echelon133.matchservice.team.service.TeamPlayerService;
import ml.echelon133.matchservice.team.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.validation.ConstraintValidator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class TeamControllerTests {

    private MockMvc mvc;

    // used by @CoachExists.Validator
    @Mock
    private CoachRepository coachRepository;

    // used by @PlayerExists.Validator
    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TeamService teamService;

    @Mock
    private TeamPlayerService teamPlayerService;

    @Mock
    private MatchService matchService;

    @InjectMocks
    private TeamExceptionHandler teamExceptionHandler;

    @InjectMocks
    private TeamController teamController;

    private JacksonTester<TeamDto> jsonTeamDto;

    private JacksonTester<UpsertTeamDto> jsonUpsertTeamDto;

    private JacksonTester<TeamPlayerDto> jsonTeamPlayerDto;

    private JacksonTester<List<TeamPlayerDto>> jsonTeamPlayerDtos;

    private JacksonTester<UpsertTeamPlayerDto> jsonUpsertTeamPlayerDto;

    private JacksonTester<List<TeamFormDto>> jsonTeamFormDtos;

    @BeforeEach
    public void beforeEach() {
        // validators with mocked dependencies which should be used by the standalone MockMvc configuration
        // every time a custom constraint validator is requested
        Map<Class<? extends ConstraintValidator>, ? extends ConstraintValidator> customValidators = Map.of(
                CoachExists.Validator.class, new CoachExists.Validator(coachRepository),
                PlayerExists.Validator.class, new PlayerExists.Validator(playerRepository)
        );
        var validatorFactoryBean = TestValidatorFactory.getInstance(customValidators);

        // use a mapper with date/time modules, otherwise LocalDate won't work
        var om = MatchServiceApplication.objectMapper();

        JacksonTester.initFields(this, om);
        mvc = MockMvcBuilders.standaloneSetup(teamController)
                .setControllerAdvice(teamExceptionHandler)
                .setCustomArgumentResolvers(
                        // required while testing controller methods which use Pageable
                        new PageableHandlerMethodArgumentResolver()
                )
                .setValidator(validatorFactoryBean)
                .build();
    }

    @Test
    @DisplayName("GET /api/teams/:id returns 404 when resource not found")
    public void getTeamById_TeamNotFound_StatusNotFound() throws Exception {
        var teamId = UUID.randomUUID();

        // given
        given(teamService.findById(teamId)).willThrow(
                new ResourceNotFoundException(Team.class, teamId)
        );

        mvc.perform(
                        get("/api/teams/" + teamId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("team %s could not be found", teamId)
                )));
    }

    @Test
    @DisplayName("GET /api/teams/:id returns 200 and a valid entity if entity found")
    public void getTeamById_TeamFound_StatusOk() throws Exception {
        var teamId = UUID.randomUUID();

        var teamDto = TestTeamDto.builder().build();
        var expectedJson = jsonTeamDto.write(teamDto).getJson();

        // given
        given(teamService.findById(teamId)).willReturn(teamDto);

        mvc.perform(
                        get("/api/teams/" + teamId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("POST /api/teams returns 422 when name is not provided")
    public void createTeam_NameNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertTeamDto.builder().name(null).build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("name", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/teams returns 422 when name length is incorrect")
    public void createTeam_NameLengthIncorrect_StatusUnprocessableEntity() throws Exception {
        var incorrectNameLengths = List.of(
                // too short (0 characters)
                "",
                /// too long (201 characters)
                "Xc9f0Gs7BSxW0SWDcEMz6vrM6e970ZQEB6LnTW3sIqtZwZOdcqAl2gvNvn2huYpPwCDnu7td5cFjAUXJaZDmaZ37oJSAKkTthS6hch6qhOJDQpvwISuXgLCHHrjl9VGRPInGCCla0yQ1ZkLVEYsjDUQ2RkrYFTi0wRvf75KXxvcLXqC7DoDGMPNpvvcy1Vh7WxaP3F4CQ"
        );

        for (String incorrectName : incorrectNameLengths) {
            var contentDto = TestUpsertTeamDto.builder().name(incorrectName).build();
            var json = jsonUpsertTeamDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/teams")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("name", List.of("expected length between 1 and 200")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/teams returns 200 when name length is correct")
    public void createTeam_NameLengthCorrect_StatusOk() throws Exception {
        var correctNameLengths = List.of(
                // minimum 1 character
                "a",
                /// maximum 200 characters
                "Xc9f0Gs7BSxW0SWDcEMz6vrM6e970ZQEB6LnTW3sIqtZwZOdcqAl2gvNvn2huYpPwCDnu7td5cFjAUXJaZDmaZ37oJSAKkTthS6hch6qhOJDQpvwISuXgLCHHrjl9VGRPInGCCla0yQ1ZkLVEYsjDUQ2RkrYFTi0wRvf75KXxvcLXqC7DoDGMPNpvvcy1Vh7WxaP3F4C"
        );

        // given
        given(coachRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

        // when
        for (String correctName : correctNameLengths) {
            var contentDto = TestUpsertTeamDto.builder().name(correctName).build();
            var bodyJson = jsonUpsertTeamDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/teams")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(bodyJson)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/teams returns 422 when crestUrl is not provided")
    public void createTeam_CrestUrlNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertTeamDto.builder().crestUrl(null).build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("crestUrl", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/teams returns 422 when crestUrl is not a valid url")
    public void createTeam_CrestUrlInvalidUrl_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertTeamDto.builder().crestUrl("a").build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("crestUrl", List.of("not a valid url")))
                );
    }

    @Test
    @DisplayName("POST /api/teams returns 422 when countryCode is not provided")
    public void createTeam_CountryIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertTeamDto.builder().countryCode(null).build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("countryCode", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/teams returns 422 when the countryCode is not valid")
    public void createTeam_CountryCodeNotValid_StatusUnprocessableEntity() throws Exception {
        var countryCodes = List.of("AAA", "BBB", "CCC");

        for (String countryCode : countryCodes) {
            var contentDto = TestUpsertTeamDto.builder().countryCode(countryCode).build();
            var json = jsonUpsertTeamDto.write(contentDto).getJson();

            // given
            given(coachRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

            // when
            mvc.perform(
                            post("/api/teams")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("countryCode", List.of("invalid ISO 3166-1 alpha-2 country code")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/teams returns 422 when coachId is not provided")
    public void createTeam_CoachIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertTeamDto.builder().coachId(null).build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("coachId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/teams returns 422 when coachId is not a uuid")
    public void createTeam_CoachIdInvalidUuid_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertTeamDto.builder().coachId("a").build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("coachId", List.of("id does not belong to a valid coach")))
                );
    }

    @Test
    @DisplayName("POST /api/teams returns 422 when the coachId does not reference an existing entity")
    public void createTeam_CoachDoesNotExist_StatusUnprocessableEntity() throws Exception {
        var coachId = UUID.randomUUID();
        var contentDto = TestUpsertTeamDto.builder().coachId(coachId.toString()).build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        // given
        given(coachRepository.existsByIdAndDeletedFalse(coachId)).willReturn(false);

        // when
        mvc.perform(
                        post("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("coachId", List.of("id does not belong to a valid coach")))
                );
    }

    @Test
    @DisplayName("POST /api/teams returns 200 and calls the service when request body valid")
    public void createTeam_ValuesInBodyCorrect_StatusOkAndCallsService() throws Exception {
        var contentDto = TestUpsertTeamDto.builder().build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        var expectedDto = TestTeamDto.builder()
                .name(contentDto.name())
                .build();
        var expectedJson = jsonTeamDto.write(expectedDto).getJson();

        // given
        given(coachRepository.existsByIdAndDeletedFalse(any())).willReturn(true);
        given(teamService.createTeam(argThat(a ->
                a.name().equals(contentDto.name()) &&
                        a.crestUrl().equals(contentDto.crestUrl()) &&
                        a.countryCode().equals(contentDto.countryCode()) &&
                        a.coachId().equals(contentDto.coachId())
        ))).willReturn(expectedDto);

        mvc.perform(
                        post("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("DELETE /api/teams/:id returns 200 and a counter of how many teams have been deleted")
    public void deleteTeam_TeamIdProvided_StatusOkAndReturnsCounter() throws Exception {
        var id = UUID.randomUUID();

        // given
        given(teamService.markTeamAsDeleted(id)).willReturn(1);

        mvc.perform(
                        delete("/api/teams/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("{\"deleted\":1}"));
    }

    @Test
    @DisplayName("PUT /api/teams/:id returns 404 when resource not found")
    public void updateTeam_TeamNotFound_StatusNotFound() throws Exception {
        var teamId = UUID.randomUUID();
        var upsertDto = TestUpsertTeamDto.builder().build();
        var upsertJson = jsonUpsertTeamDto.write(upsertDto).getJson();

        // given
        given(coachRepository.existsByIdAndDeletedFalse(any())).willReturn(true);
        given(teamService.updateTeam(eq(teamId), any())).willThrow(
                new ResourceNotFoundException(Team.class, teamId)
        );

        mvc.perform(
                        put("/api/teams/" + teamId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(upsertJson)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("team %s could not be found", teamId)
                )));
    }

    @Test
    @DisplayName("PUT /api/teams/:id returns 422 when name is not provided")
    public void updateTeam_NameNotProvided_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var contentDto = TestUpsertTeamDto.builder().name(null).build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/teams/" + teamId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("name", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("PUT /api/teams/:id returns 422 when name length is incorrect")
    public void updateTeam_NameLengthIncorrect_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var incorrectNameLengths = List.of(
                // too short (0 characters)
                "",
                /// too long (201 characters)
                "Xc9f0Gs7BSxW0SWDcEMz6vrM6e970ZQEB6LnTW3sIqtZwZOdcqAl2gvNvn2huYpPwCDnu7td5cFjAUXJaZDmaZ37oJSAKkTthS6hch6qhOJDQpvwISuXgLCHHrjl9VGRPInGCCla0yQ1ZkLVEYsjDUQ2RkrYFTi0wRvf75KXxvcLXqC7DoDGMPNpvvcy1Vh7WxaP3F4CQ"
        );

        for (String incorrectName : incorrectNameLengths) {
            var contentDto = TestUpsertTeamDto.builder().name(incorrectName).build();
            var json = jsonUpsertTeamDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/teams/" + teamId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("name", List.of("expected length between 1 and 200")))
                    );
        }
    }

    @Test
    @DisplayName("PUT /api/teams/:id returns 200 when name length is correct")
    public void updateTeam_NameLengthCorrect_StatusOk() throws Exception {
        var teamId = UUID.randomUUID();
        var correctNameLengths = List.of(
                // minimum 1 character
                "a",
                /// maximum 200 characters
                "Xc9f0Gs7BSxW0SWDcEMz6vrM6e970ZQEB6LnTW3sIqtZwZOdcqAl2gvNvn2huYpPwCDnu7td5cFjAUXJaZDmaZ37oJSAKkTthS6hch6qhOJDQpvwISuXgLCHHrjl9VGRPInGCCla0yQ1ZkLVEYsjDUQ2RkrYFTi0wRvf75KXxvcLXqC7DoDGMPNpvvcy1Vh7WxaP3F4C"
        );

        // given
        given(coachRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

        // when
        for (String correctName : correctNameLengths) {
            var contentDto = TestUpsertTeamDto.builder().name(correctName).build();
            var bodyJson = jsonUpsertTeamDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/teams/" + teamId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(bodyJson)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("PUT /api/teams/:id returns 422 when crestUrl is not provided")
    public void updateTeam_CrestUrlNotProvided_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var contentDto = TestUpsertTeamDto.builder().crestUrl(null).build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/teams/" + teamId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("crestUrl", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("PUT /api/teams/:id returns 422 when crestUrl is not a valid url")
    public void updateTeam_CrestUrlInvalidUrl_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var contentDto = TestUpsertTeamDto.builder().crestUrl("a").build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/teams/" + teamId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("crestUrl", List.of("not a valid url")))
                );
    }

    @Test
    @DisplayName("PUT /api/teams/:id returns 422 when countryCode is not provided")
    public void updateTeam_CountryIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var contentDto = TestUpsertTeamDto.builder().countryCode(null).build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/teams/" + teamId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("countryCode", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("PUT /api/teams/:id returns 422 when the countryCode is not valid")
    public void updateTeam_CountryCodeNotValid_StatusUnprocessableEntity() throws Exception {
        var countryCodes = List.of("AAA", "BBB", "CCC");

        for (String countryCode : countryCodes) {
            var teamId = UUID.randomUUID();
            var contentDto = TestUpsertTeamDto.builder().countryCode(countryCode).build();
            var json = jsonUpsertTeamDto.write(contentDto).getJson();

            // given
            given(coachRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

            // when
            mvc.perform(
                            put("/api/teams/" + teamId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("countryCode", List.of("invalid ISO 3166-1 alpha-2 country code")))
                    );
        }
    }

    @Test
    @DisplayName("PUT /api/teams/:id returns 422 when coachId is not provided")
    public void updateTeam_CoachIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var contentDto = TestUpsertTeamDto.builder().coachId(null).build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/teams/" + teamId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("coachId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("PUT /api/teams/:id returns 422 when coachId is not a uuid")
    public void updateTeam_CoachIdInvalidUuid_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var contentDto = TestUpsertTeamDto.builder().coachId("a").build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/teams/" + teamId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("coachId", List.of("id does not belong to a valid coach")))
                );
    }

    @Test
    @DisplayName("PUT /api/teams/:id returns 422 when the coachId does not reference an existing entity")
    public void updateTeam_CoachDoesNotExist_StatusUnprocessableEntity() throws Exception {
        var coachId = UUID.randomUUID();
        var teamId = UUID.randomUUID();
        var contentDto = TestUpsertTeamDto.builder().coachId(coachId.toString()).build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        // given
        given(coachRepository.existsByIdAndDeletedFalse(coachId)).willReturn(false);

        // when
        mvc.perform(
                        put("/api/teams/" + teamId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("coachId", List.of("id does not belong to a valid coach")))
                );
    }

    @Test
    @DisplayName("PUT /api/teams/:id returns 200 and calls the service when request body valid")
    public void updateTeam_ValuesInBodyCorrect_StatusOkAndCallsService() throws Exception {
        var teamId = UUID.randomUUID();
        var contentDto = TestUpsertTeamDto.builder().build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        var expectedDto = TestTeamDto.builder()
                .name(contentDto.name())
                .build();
        var expectedJson = jsonTeamDto.write(expectedDto).getJson();

        // given
        given(coachRepository.existsByIdAndDeletedFalse(any())).willReturn(true);
        given(teamService.updateTeam(
                eq(teamId),
                argThat(a ->
                        a.name().equals(contentDto.name()) &&
                                a.crestUrl().equals(contentDto.crestUrl()) &&
                                a.countryCode().equals(contentDto.countryCode()) &&
                                a.coachId().equals(contentDto.coachId())
                ))).willReturn(expectedDto);

        // when
        mvc.perform(
                        put("/api/teams/" + teamId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("GET /api/teams returns 400 when neither `name` nor 'teamIds' are provided")
    public void getTeamsByCriteria_NameAndTeamIdsNotProvided_StatusBadRequest() throws Exception {
        mvc.perform(
                        get("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", containsInAnyOrder(
                        "query parameter 'name' not provided",
                        "query parameter 'teamIds' not provided"
                )));
    }

    @Test
    @DisplayName("GET /api/teams returns 400 when both `name` and 'teamIds' are provided")
    public void getTeamsByCriteria_NameAndTeamIdsProvided_StatusBadRequest() throws Exception {
        mvc.perform(
                        get("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("name", "test")
                                .param("teamIds", String.format("%s,%s", UUID.randomUUID(), UUID.randomUUID()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", containsInAnyOrder(
                        "query parameter 'name' cannot be provided together with 'teamIds'",
                        "query parameter 'teamIds' cannot be provided together with 'name'"
                )));
    }

    @Test
    @DisplayName("GET /api/teams returns 200 when only `name` is provided and pageable is default")
    public void getTeamsByCriteria_NameProvidedWithDefaultPageable_StatusOk() throws Exception {
        var pValue = "test";
        var defaultPageNumber = 0;
        var defaultPageSize = 20;
        var expectedPageable = Pageable.ofSize(defaultPageSize).withPage(defaultPageNumber);

        Page<TeamDto> expectedPage = Page.empty(expectedPageable);

        //given
        given(teamService.findTeamsByName(
                eq(pValue),
                argThat(p -> p.getPageSize() == defaultPageSize && p.getPageNumber() == defaultPageNumber)
        )).willReturn(expectedPage);

        mvc.perform(
                        get("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("name", pValue)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(0)));
    }

    @Test
    @DisplayName("GET /api/teams returns 200 when only `name` is provided and pageable values are custom")
    public void getTeamsByCriteria_NameProvidedWithCustomPageParameters_StatusOk() throws Exception {
        var pValue = "test";
        var testPageSize = 7;
        var testPageNumber = 4;
        var expectedPageable = Pageable.ofSize(testPageSize).withPage(testPageNumber);
        var expectedContent = List.of(TestTeamDto.builder().name(pValue).build());

        Page<TeamDto> expectedPage = new PageImpl<>(expectedContent, expectedPageable, 1);

        //given
        given(teamService.findTeamsByName(
                eq(pValue),
                argThat(p -> p.getPageNumber() == testPageNumber && p.getPageSize() == testPageSize)
        )).willReturn(expectedPage);

        mvc.perform(
                        get("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("name", pValue)
                                .param("page", String.valueOf(testPageNumber))
                                .param("size", String.valueOf(testPageSize))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(1)))
                .andExpect(jsonPath("$.content[0].name", is(pValue)));
    }

    @Test
    @DisplayName("GET /api/teams returns 200 when only `teamIds` is provided and pageable is default")
    public void getTeamsByCriteria_TeamIdsProvidedWithDefaultPageable_StatusOk() throws Exception {
        var defaultPageNumber = 0;
        var defaultPageSize = 20;
        var expectedPageable = Pageable.ofSize(defaultPageSize).withPage(defaultPageNumber);

        Page<TeamDto> expectedPage = Page.empty(expectedPageable);

        var requestedTeamId = UUID.randomUUID();

        //given
        given(teamService.findTeamsByIds(
                eq(List.of(requestedTeamId)),
                argThat(p -> p.getPageSize() == defaultPageSize && p.getPageNumber() == defaultPageNumber)
        )).willReturn(expectedPage);

        mvc.perform(
                        get("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("teamIds", requestedTeamId.toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(0)));
    }

    @Test
    @DisplayName("GET /api/teams returns 200 when only `teamIds` is provided and pageable values are custom")
    public void getTeamsByCriteria_TeamIdsProvidedWithCustomPageParameters_StatusOk() throws Exception {
        var requestedTeamId = UUID.randomUUID();
        var testPageSize = 7;
        var testPageNumber = 4;
        var expectedPageable = Pageable.ofSize(testPageSize).withPage(testPageNumber);
        var expectedContent = List.of(TestTeamDto.builder().id(requestedTeamId).build());

        Page<TeamDto> expectedPage = new PageImpl<>(expectedContent, expectedPageable, 1);

        //given
        given(teamService.findTeamsByIds(
                eq(List.of(requestedTeamId)),
                argThat(p -> p.getPageNumber() == testPageNumber && p.getPageSize() == testPageSize)
        )).willReturn(expectedPage);

        mvc.perform(
                        get("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("teamIds", requestedTeamId.toString())
                                .param("page", String.valueOf(testPageNumber))
                                .param("size", String.valueOf(testPageSize))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(1)))
                .andExpect(jsonPath("$.content[0].id", is(requestedTeamId.toString())));
    }

    @Test
    @DisplayName("GET /api/teams/:id/players returns 200 when team is found")
    public void getTeamPlayers_TeamFound_StatusOk() throws Exception {
        var teamId = UUID.randomUUID();
        var teamPlayers = List.of(
                TestTeamPlayerDto.builder().build()
        );

        var expectedJson = jsonTeamPlayerDtos.write(teamPlayers).getJson();

        // given
        given(teamPlayerService.findAllPlayersOfTeam(teamId)).willReturn(teamPlayers);

        mvc.perform(
                        get("/api/teams/" + teamId + "/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("POST /api/teams/:id/players returns 404 when the service throws ResourceNotFoundException caused by Team")
    public void assignPlayerToTeam_ServiceThrowsWhenTeamNotFound_StatusNotFound() throws Exception {
        var teamId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var contentDto = new UpsertTeamPlayerDto(playerId.toString(), "FORWARD", 1);
        var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

        // given
        given(playerRepository.existsByIdAndDeletedFalse(playerId)).willReturn(true);
        given(teamPlayerService.createTeamPlayer(
                eq(teamId),
                argThat(a ->
                        a.playerId().equals(playerId.toString()) &&
                                a.number().equals(contentDto.number()) &&
                                a.position().equals(contentDto.position())
                )
        )).willThrow(new ResourceNotFoundException(Team.class, teamId));

        // when
        mvc.perform(
                        post("/api/teams/" + teamId + "/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("team %s could not be found", teamId)
                )));
    }

    @Test
    @DisplayName("POST /api/teams/:id/players returns 422 when playerId is not provided")
    public void assignPlayerToTeam_PlayerIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var contentDto = new UpsertTeamPlayerDto(null, "FORWARD", 1);
        var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/teams/" + teamId + "/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("playerId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/teams/:id/players returns 422 when playerId is not a uuid")
    public void assignPlayerToTeam_PlayerIdInvalidUuid_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var contentDto = new UpsertTeamPlayerDto("a", "FORWARD", 1);
        var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/teams/" + teamId + "/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("playerId", List.of("id does not belong to a valid player")))
                );
    }

    @Test
    @DisplayName("POST /api/teams/:id/players returns 422 when the playerId does not reference an existing entity")
    public void assignPlayerToTeam_PlayerDoesNotExist_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var contentDto = new UpsertTeamPlayerDto(playerId.toString(), "FORWARD", 1);
        var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

        // given
        given(playerRepository.existsByIdAndDeletedFalse(playerId)).willReturn(false);

        // when
        mvc.perform(
                        post("/api/teams/" + teamId + "/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("playerId", List.of("id does not belong to a valid player")))
                );
    }

    @Test
    @DisplayName("POST /api/teams/:id/players returns 422 when position is not provided")
    public void assignPlayerToTeam_PositionNotProvided_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var contentDto = new UpsertTeamPlayerDto(UUID.randomUUID().toString(), null, 1);
        var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/teams/" + teamId + "/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("position", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/teams/:id/players returns 422 when position value is incorrect")
    public void assignPlayerToTeam_PositionIncorrect_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();

        var incorrectPositions = List.of(
                "asdf", "", "TEST", "test", "aaaaaaaaaaaaaaaaaaaaaa"
        );

        for (String incorrectPosition : incorrectPositions) {
            var contentDto = new UpsertTeamPlayerDto(UUID.randomUUID().toString(), incorrectPosition, 1);
            var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/teams/" + teamId + "/players")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.messages", hasEntry("position", List.of("required exactly one of [GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD]"))));
        }
    }

    @Test
    @DisplayName("POST /api/teams/:id/players returns 200 when position's value is correct")
    public void assignPlayerToTeam_PositionCorrect_StatusOk() throws Exception {
        var teamId = UUID.randomUUID();

        var correctPositions = List.of(
                "GOALKEEPER", "DEFENDER", "MIDFIELDER", "FORWARD", // uppercase
                "goalkeeper", "defender", "midfielder", "forward", // lowercase
                "GOALkeeper", "DEFender", "MIDfielder", "FORward" // mixed
        );

        // given
        given(playerRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

        // when
        for (String correctPosition: correctPositions) {
            var contentDto = new UpsertTeamPlayerDto(UUID.randomUUID().toString(), correctPosition, 1);
            var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/teams/" + teamId + "/players")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/teams/:id/players returns 422 when number is not provided")
    public void assignPlayerToTeam_NumberNotProvided_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var contentDto = new UpsertTeamPlayerDto(UUID.randomUUID().toString(), "DEFENDER", null);
        var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/teams/" + teamId + "/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("number", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/teams/:id/players returns 422 when number's value is incorrect")
    public void assignPlayerToTeam_NumberIncorrect_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();

        var incorrectNumbers = List.of(
                0, 100, 200, 1000, 100000
        );

        for (Integer incorrectNumber : incorrectNumbers) {
            var contentDto = new UpsertTeamPlayerDto(UUID.randomUUID().toString(), "DEFENDER", incorrectNumber);
            var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/teams/" + teamId + "/players")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.messages", hasEntry("number", List.of("expected number between 1 and 99"))));
        }
    }

    @Test
    @DisplayName("POST /api/teams/:id/players returns 422 when the service throws NumberAlreadyTakenException")
    public void assignPlayerToTeam_ServiceThrowsWhenNumberTaken_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var number = 1;
        var contentDto = new UpsertTeamPlayerDto(playerId.toString(), "FORWARD", number);
        var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

        // given
        given(playerRepository.existsByIdAndDeletedFalse(playerId)).willReturn(true);
        given(teamPlayerService.createTeamPlayer(
                eq(teamId),
                argThat(a ->
                        a.playerId().equals(playerId.toString()) &&
                                a.number().equals(contentDto.number()) &&
                                a.position().equals(contentDto.position())
                )
        )).willThrow(new NumberAlreadyTakenException(teamId, number));

        // when
        var expectedError = String.format("team %s already has a player with number %d", teamId, number);
        mvc.perform(
                        post("/api/teams/" + teamId + "/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("number", List.of(expectedError)))
                );
    }

    @Test
    @DisplayName("POST /api/teams/:id/players returns 200 when number's value is correct")
    public void assignPlayerToTeam_NumberCorrect_StatusOk() throws Exception {
        var teamId = UUID.randomUUID();

        var correctNumbers = List.of(
                1, 20, 50, 85, 99
        );

        // given
        given(playerRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

        for (Integer correctNumber : correctNumbers) {
            var contentDto = new UpsertTeamPlayerDto(UUID.randomUUID().toString(), "DEFENDER", correctNumber);
            var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/teams/" + teamId + "/players")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/teams/:id/players returns 200 with correct body")
    public void assignPlayerToTeam_ValuesInBodyCorrect_StatusOkAndCallsService() throws Exception {
        var teamId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var contentDto = new UpsertTeamPlayerDto(playerId.toString(), "DEFENDER", 1);
        var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

        var expectedDto = TestTeamPlayerDto
                .builder()
                .playerId(playerId)
                .position(contentDto.position())
                .number(contentDto.number()).build();
        var expectedJson = jsonTeamPlayerDto.write(expectedDto).getJson();

        // given
        given(playerRepository.existsByIdAndDeletedFalse(playerId)).willReturn(true);
        given(teamPlayerService.createTeamPlayer(
                eq(teamId),
                argThat(a ->
                        a.playerId().equals(playerId.toString()) &&
                                a.position().equals(contentDto.position()) &&
                                a.number().equals(contentDto.number())
                )
        )).willReturn(expectedDto);


        // when
        mvc.perform(
                        post("/api/teams/" + teamId + "/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("DELETE /api/teams/:id/players/:teamPlayerId returns 200 and a counter of how many players have been deleted")
    public void deletePlayerAssignment_TeamPlayerIdProvided_StatusOkAndReturnsCounter() throws Exception {
        var teamPlayerId = UUID.randomUUID();
        var teamId = UUID.randomUUID();

        // given
        given(teamPlayerService.markTeamPlayerAsDeleted(teamPlayerId)).willReturn(1);

        // when
        mvc.perform(
                        delete("/api/teams/" + teamId + "/players/" + teamPlayerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("{\"deleted\":1}"));
    }

    @Test
    @DisplayName("PUT /api/teams/:id/players/:teamPlayerId returns 404 when the service throws ResourceNotFoundException caused by Team")
    public void updatePlayerOfTeam_ServiceThrowsWhenTeamNotFound_StatusNotFound() throws Exception {
        var teamId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var teamPlayerId = UUID.randomUUID();
        var contentDto = new UpsertTeamPlayerDto(playerId.toString(), "FORWARD", 1);
        var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

        // given
        given(playerRepository.existsByIdAndDeletedFalse(playerId)).willReturn(true);
        given(teamPlayerService.updateTeamPlayer(
                eq(teamPlayerId),
                eq(teamId),
                argThat(a ->
                        a.playerId().equals(playerId.toString()) &&
                                a.number().equals(contentDto.number()) &&
                                a.position().equals(contentDto.position())
                )
        )).willThrow(new ResourceNotFoundException(Team.class, teamId));

        // when
        mvc.perform(
                        put("/api/teams/" + teamId + "/players/" + teamPlayerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("team %s could not be found", teamId)
                )));
    }

    @Test
    @DisplayName("PUT /api/teams/:id/players/:teamPlayerId returns 422 when playerId is not provided")
    public void updatePlayerOfTeam_PlayerIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var teamPlayerId = UUID.randomUUID();
        var contentDto = new UpsertTeamPlayerDto(null, "FORWARD", 1);
        var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/teams/" + teamId + "/players/" + teamPlayerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("playerId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("PUT /api/teams/:id/players/:teamPlayerId returns 422 when playerId is not a uuid")
    public void updatePlayerOfTeam_PlayerIdInvalidUuid_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var teamPlayerId = UUID.randomUUID();
        var contentDto = new UpsertTeamPlayerDto("a", "FORWARD", 1);
        var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/teams/" + teamId + "/players/" + teamPlayerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("playerId", List.of("id does not belong to a valid player")))
                );
    }

    @Test
    @DisplayName("PUT /api/teams/:id/players/:teamPlayerId returns 422 when the playerId does not reference an existing entity")
    public void updatePlayerOfTeam_PlayerDoesNotExist_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var teamPlayerId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var contentDto = new UpsertTeamPlayerDto(playerId.toString(), "FORWARD", 1);
        var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

        // given
        given(playerRepository.existsByIdAndDeletedFalse(playerId)).willReturn(false);

        // when
        mvc.perform(
                        put("/api/teams/" + teamId + "/players/" + teamPlayerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("playerId", List.of("id does not belong to a valid player")))
                );
    }

    @Test
    @DisplayName("PUT /api/teams/:id/players/:teamPlayerId returns 422 when position is not provided")
    public void updatePlayerOfTeam_PositionNotProvided_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var teamPlayerId = UUID.randomUUID();
        var contentDto = new UpsertTeamPlayerDto(UUID.randomUUID().toString(),null, 1);
        var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/teams/" + teamId + "/players/" + teamPlayerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("position", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("PUT /api/teams/:id/players/:teamPlayerId returns 422 when position value is incorrect")
    public void updatePlayerOfTeam_PositionIncorrect_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var teamPlayerId = UUID.randomUUID();

        var incorrectPositions = List.of(
                "asdf", "", "TEST", "test", "aaaaaaaaaaaaaaaaaaaaaa"
        );

        for (String incorrectPosition : incorrectPositions) {
            var contentDto = new UpsertTeamPlayerDto(UUID.randomUUID().toString(), incorrectPosition, 1);
            var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/teams/" + teamId + "/players/" + teamPlayerId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("position", List.of("required exactly one of [GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD]")))
                    );
        }
    }

    @Test
    @DisplayName("PUT /api/teams/:id/players/:teamPlayerId returns 422 when position value is correct")
    public void updatePlayerOfTeam_PositionCorrect_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var teamPlayerId = UUID.randomUUID();

        var correctPositions = List.of(
                "GOALKEEPER", "DEFENDER", "MIDFIELDER", "FORWARD", // uppercase
                "goalkeeper", "defender", "midfielder", "forward", // lowercase
                "GOALkeeper", "DEFender", "MIDfielder", "FORward" // mixed
        );

        // given
        given(playerRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

        // when
        for (String correctPosition: correctPositions) {
            var contentDto = new UpsertTeamPlayerDto(UUID.randomUUID().toString(), correctPosition, 1);
            var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/teams/" + teamId + "/players/" + teamPlayerId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("PUT /api/teams/:id/players/:teamPlayerId returns 422 when number is not provided")
    public void updatePlayerOfTeam_NumberNotProvided_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var teamPlayerId = UUID.randomUUID();
        var contentDto = new UpsertTeamPlayerDto(UUID.randomUUID().toString(),"DEFENDER", null);
        var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/teams/" + teamId + "/players/" + teamPlayerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("number", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("PUT /api/teams/:id/players/:teamPlayerId returns 422 when number value is incorrect")
    public void updatePlayerOfTeam_NumberIncorrect_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var teamPlayerId = UUID.randomUUID();

        var incorrectNumbers = List.of(
                0, 100, 200, 1000, 100000
        );

        for (Integer incorrectNumber: incorrectNumbers) {
            var contentDto = new UpsertTeamPlayerDto(UUID.randomUUID().toString(), "DEFENDER", incorrectNumber);
            var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/teams/" + teamId + "/players/" + teamPlayerId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("number", List.of("expected number between 1 and 99")))
                    );
        }
    }

    @Test
    @DisplayName("PUT /api/teams/:id/players/:teamPlayerId returns 200 when number value is correct")
    public void updatePlayerOfTeam_NumberCorrect_StatusOk() throws Exception {
        var teamId = UUID.randomUUID();
        var teamPlayerId = UUID.randomUUID();

        var correctNumbers = List.of(
                1, 20, 50, 85, 99
        );

        // given
        given(playerRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

        // when
        for (Integer correctNumber: correctNumbers) {
            var contentDto = new UpsertTeamPlayerDto(UUID.randomUUID().toString(), "DEFENDER", correctNumber);
            var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/teams/" + teamId + "/players/" + teamPlayerId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("PUT /api/teams/:id/players/:teamPlayerId returns 422 when the service throws NumberAlreadyTakenException")
    public void updatePlayerOfTeam_ServiceThrowsWhenNumberTaken_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var teamPlayerId = UUID.randomUUID();
        var number = 1;
        var contentDto = new UpsertTeamPlayerDto(playerId.toString(), "FORWARD", number);
        var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

        // given
        given(playerRepository.existsByIdAndDeletedFalse(playerId)).willReturn(true);
        given(teamPlayerService.updateTeamPlayer(
                eq(teamPlayerId),
                eq(teamId),
                argThat(a ->
                        a.playerId().equals(playerId.toString()) &&
                                a.number().equals(contentDto.number()) &&
                                a.position().equals(contentDto.position())
                )
        )).willThrow(new NumberAlreadyTakenException(teamId, number));

        // when
        var expectedError = String.format("team %s already has a player with number %d", teamId, number);
        mvc.perform(
                        put("/api/teams/" + teamId + "/players/" + teamPlayerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("number", List.of(expectedError)))
                );
    }

    @Test
    @DisplayName("PUT /api/teams/:id/players/:teamPlayerId returns 200 with correct body")
    public void updatePlayerOfTeam_ValuesInBodyCorrect_StatusOkAndCallsService() throws Exception {
        var teamId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var teamPlayerId = UUID.randomUUID();
        var contentDto = new UpsertTeamPlayerDto(playerId.toString(), "DEFENDER", 1);
        var json = jsonUpsertTeamPlayerDto.write(contentDto).getJson();

        var expectedDto = TestTeamPlayerDto
                .builder()
                .playerId(playerId)
                .position(contentDto.position())
                .number(contentDto.number()).build();
        var expectedJson = jsonTeamPlayerDto.write(expectedDto).getJson();

        // given
        given(playerRepository.existsByIdAndDeletedFalse(playerId)).willReturn(true);
        given(teamPlayerService.updateTeamPlayer(
                eq(teamPlayerId),
                eq(teamId),
                argThat(a ->
                        a.playerId().equals(playerId.toString()) &&
                                a.position().equals(contentDto.position()) &&
                                a.number().equals(contentDto.number())
                )
        )).willReturn(expectedDto);


        // when
        mvc.perform(
                        put("/api/teams/" + teamId + "/players/" + teamPlayerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("GET /api/teams/:id/form?competitionId= returns 200 when 'competitionId' is provided and the service contains the form")
    public void getTeamForm_CompetitionIdProvided_StatusOk() throws Exception {
        var teamId = UUID.randomUUID();
        var competitionId = UUID.randomUUID();

        var formEntry = new TeamFormDto(
                'W',
                TeamFormDetailsDto.from(
                        UUID.randomUUID(),
                        LocalDateTime.now(),
                        new ShortTeamDto(teamId, "Some team", ""),
                        new ShortTeamDto(UUID.randomUUID(), "Some other team", ""),
                        new ScoreInfoDto(3, 2)
                )
        );
        var expectedJson = jsonTeamFormDtos.write(List.of(formEntry)).getJson();

        // given
        given(teamService.evaluateForm(teamId, competitionId)).willReturn(List.of(formEntry));

        // when
        mvc.perform(
                        get("/api/teams/" + teamId + "/form")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("competitionId", competitionId.toString())
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("GET /api/teams/:id/form returns 200 when 'competitionId' is not provided and the service contains the form")
    public void getTeamForm_CompetitionIdNotProvided_StatusOk() throws Exception {
        var teamId = UUID.randomUUID();

        var formEntry = new TeamFormDto(
                'W',
                TeamFormDetailsDto.from(
                        UUID.randomUUID(),
                        LocalDateTime.now(),
                        new ShortTeamDto(teamId, "Some team", ""),
                        new ShortTeamDto(UUID.randomUUID(), "Some other team", ""),
                        new ScoreInfoDto(3, 2)
                )
        );
        var expectedJson = jsonTeamFormDtos.write(List.of(formEntry)).getJson();

        // given
        given(teamService.evaluateGeneralForm(teamId)).willReturn(List.of(formEntry));

        // when
        mvc.perform(
                        get("/api/teams/" + teamId + "/form")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("GET /api/teams/:teamId/matches returns 400 when 'type' not provided")
    public void getTeamMatches_TypeNotProvided_StatusBadRequest() throws Exception {
        var teamId = UUID.randomUUID();

        mvc.perform(
                get("/api/teams/" + teamId + "/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                ).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasItems(
                        "query parameter 'type' not provided"
                )));
    }

    @Test
    @DisplayName("GET /api/teams/:teamId/matches returns 400 when 'type' value is incorrect")
    public void getTeamMatches_TypeIncorrect_StatusBadRequest() throws Exception {
        var teamId = UUID.randomUUID();

        var incorrectTypes = List.of("asdf", "test", "fixture", "result");

        for (var incorrectType: incorrectTypes) {
            mvc.perform(
                            get("/api/teams/" + teamId + "/matches")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .param("type", incorrectType)
                    ).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messages", hasItems(
                            "query parameter 'type' should be either 'fixtures' or 'results'"
                    )));
        }
    }

    @Test
    @DisplayName("GET /api/teams/:teamId/matches returns 200 when 'type' value is 'fixtures' (case insensitive)")
    public void getTeamMatches_TypeFixturesCaseInsensitive_StatusOk() throws Exception {
        var teamId = UUID.randomUUID();
        var competitionDto = new CompetitionDto(UUID.randomUUID(), "", "", "", true, 1, true);
        var matches = List.of(CompactMatchDto.builder().build());

        // given
        var correctTypes = List.of("FIXTURES", "fixtures", "fixTURES");
        given(matchService.findMatchesByTeam(
                eq(teamId),
                eq(false),
                eq(Pageable.ofSize(20).withPage(0))
        )).willReturn(List.of(new CompetitionGroupedMatches(competitionDto, matches)));

        // when
        for (var correctType: correctTypes) {
            mvc.perform(
                            get("/api/teams/" + teamId + "/matches")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .param("type", correctType)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    @Test
    @DisplayName("GET /api/teams/:teamId/matches returns 200 when 'type' value is 'results' (case insensitive)")
    public void getTeamMatches_TypeResultsCaseInsensitive_StatusOk() throws Exception {
        var teamId = UUID.randomUUID();
        var competitionDto = new CompetitionDto(UUID.randomUUID(), "", "", "", true, 1, true);
        var matches = List.of(CompactMatchDto.builder().build());

        // given
        var correctTypes = List.of("RESULTS", "results", "resULTS");
        given(matchService.findMatchesByTeam(
                eq(teamId),
                eq(true),
                eq(Pageable.ofSize(20).withPage(0))
        )).willReturn(List.of(new CompetitionGroupedMatches(competitionDto, matches)));

        // when
        for (var correctType: correctTypes) {
            mvc.perform(
                    get("/api/teams/" + teamId + "/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .param("type", correctType)
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    @Test
    @DisplayName("GET /api/teams/:teamId/matches returns 200 when custom 'page' and 'size' are provided alongside 'type'")
    public void getTeamMatches_TypeProvidedAndCustomPageAndSize_StatusOk() throws Exception {
        var teamId = UUID.randomUUID();
        var expectedPageSize = 27;
        var expectedPage = 3;

        // given
        var correctTypes = List.of("results", "fixtures");

        // when
        for (var correctType: correctTypes) {
            mvc.perform(
                            get("/api/teams/" + teamId + "/matches")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .param("type", correctType)
                                    .param("page", String.valueOf(expectedPage))
                                    .param("size", String.valueOf(expectedPageSize))
                    )
                    .andExpect(status().isOk());
        }

        verify(matchService, times(2)).findMatchesByTeam(
                eq(teamId),
                anyBoolean(),
                eq(Pageable.ofSize(expectedPageSize).withPage(expectedPage))
        );
    }
}