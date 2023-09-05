package ml.echelon133.matchservice.team.controller;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.team.dto.TeamDto;
import ml.echelon133.matchservice.MatchServiceApplication;
import ml.echelon133.matchservice.country.model.Country;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.team.model.UpsertTeamDto;
import ml.echelon133.matchservice.team.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class TeamControllerTests {

    private MockMvc mvc;

    @Mock
    private TeamService teamService;

    @InjectMocks
    private TeamExceptionHandler teamExceptionHandler;

    @InjectMocks
    private TeamController teamController;

    private JacksonTester<TeamDto> jsonTeamDto;

    private JacksonTester<UpsertTeamDto> jsonUpsertTeamDto;

    @BeforeEach
    public void beforeEach() {
        // use a mapper with date/time modules, otherwise LocalDate won't work
        var om = MatchServiceApplication.objectMapper();

        JacksonTester.initFields(this, om);
        mvc = MockMvcBuilders.standaloneSetup(teamController)
                .setControllerAdvice(teamExceptionHandler)
                .setCustomArgumentResolvers(
                        // required while testing controller methods which use Pageable
                        new PageableHandlerMethodArgumentResolver()
                )
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

        var teamDto = TeamDto.builder().build();
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
        var contentDto = UpsertTeamDto.builder().name(null).build();
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
            var contentDto = UpsertTeamDto.builder().name(incorrectName).build();
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

        for (String correctName : correctNameLengths) {
            var contentDto = UpsertTeamDto.builder().name(correctName).build();
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
    @DisplayName("POST /api/teams returns 422 when countryId is not provided")
    public void createTeam_CountryIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = UpsertTeamDto.builder().countryId(null).build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("countryId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/teams returns 422 when countryId is not an uuid")
    public void createTeam_CountryIdInvalidUuid_StatusUnprocessableEntity() throws Exception {
        var contentDto = UpsertTeamDto.builder().countryId("a").build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("countryId", List.of("not a valid uuid")))
                );
    }

    @Test
    @DisplayName("POST /api/teams returns 422 when the service throws ResourceNotFoundException caused by Country")
    public void createTeam_ServiceThrowsWhenCountryNotFound_StatusUnprocessableEntity() throws Exception {
        var contentDto = UpsertTeamDto.builder().countryId(UUID.randomUUID().toString()).build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();
        var countryId = UUID.fromString(contentDto.getCountryId());

        // given
        given(teamService.createTeam(argThat(a ->
                a.getName().equals(contentDto.getName()) &&
                        a.getCountryId().equals(contentDto.getCountryId())
        ))).willThrow(new ResourceNotFoundException(Country.class, countryId));

        // when
        var expectedError = String.format("country %s could not be found", countryId);
        mvc.perform(
                        post("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("countryId", List.of(expectedError)))
                );
    }

    @Test
    @DisplayName("POST /api/teams returns 200 and calls the service when request body valid")
    public void createTeam_ValuesInBodyCorrect_StatusOkAndCallsService() throws Exception {
        var contentDto = UpsertTeamDto.builder().build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        var expectedDto = TeamDto.builder()
                .name(contentDto.getName())
                .build();
        var expectedJson = jsonTeamDto.write(expectedDto).getJson();

        // given
        given(teamService.createTeam(argThat(a ->
                a.getName().equals(contentDto.getName()) && a.getCountryId().equals(contentDto.getCountryId())
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
        var upsertDto = UpsertTeamDto.builder().build();
        var upsertJson = jsonUpsertTeamDto.write(upsertDto).getJson();

        // given
        given(teamService.updateTeam(eq(teamId), ArgumentMatchers.any())).willThrow(
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
        var contentDto = UpsertTeamDto.builder().name(null).build();
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
            var contentDto = UpsertTeamDto.builder().name(incorrectName).build();
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

        for (String correctName : correctNameLengths) {
            var contentDto = UpsertTeamDto.builder().name(correctName).build();
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
    @DisplayName("PUT /api/teams/:id returns 422 when countryId is not provided")
    public void updateTeam_CountryIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var contentDto = UpsertTeamDto.builder().countryId(null).build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/teams/" + teamId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("countryId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("PUT /api/teams/:id returns 422 when countryId is not an uuid")
    public void updateTeam_CountryIdInvalidUuid_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var contentDto = UpsertTeamDto.builder().countryId("a").build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/teams/" + teamId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("countryId", List.of("not a valid uuid")))
                );
    }

    @Test
    @DisplayName("PUT /api/teams/:id returns 422 when the service throws ResourceNotFoundException caused by Country")
    public void updateTeam_ServiceThrowsWhenCountryNotFound_StatusUnprocessableEntity() throws Exception {
        var teamId = UUID.randomUUID();
        var contentDto = UpsertTeamDto.builder().build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();
        var countryId = UUID.fromString(contentDto.getCountryId());

        // given
        given(teamService.updateTeam(
                eq(teamId),
                argThat(a ->
                        a.getName().equals(contentDto.getName()) && a.getCountryId().equals(contentDto.getCountryId())
                )
        )).willThrow(new ResourceNotFoundException(Country.class, countryId));

        // when
        var expectedError = String.format("country %s could not be found", countryId);
        mvc.perform(
                        put("/api/teams/" + teamId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("countryId", List.of(expectedError)))
                );
    }

    @Test
    @DisplayName("PUT /api/teams/:id returns 200 and calls the service when request body valid")
    public void updateTeam_ValuesInBodyCorrect_StatusOkAndCallsService() throws Exception {
        var teamId = UUID.randomUUID();
        var contentDto = UpsertTeamDto.builder().build();
        var json = jsonUpsertTeamDto.write(contentDto).getJson();

        var expectedDto = TeamDto.builder()
                .name(contentDto.getName())
                .build();
        var expectedJson = jsonTeamDto.write(expectedDto).getJson();

        // given
        given(teamService.updateTeam(
                eq(teamId),
                argThat(a ->
                        a.getName().equals(contentDto.getName()) && a.getCountryId().equals(contentDto.getCountryId())
                ))).willReturn(expectedDto);

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
    @DisplayName("GET /api/teams?name= returns 400 when `name` is not provided")
    public void getTeamsByName_NameNotProvided_StatusBadRequest() throws Exception {
        mvc.perform(
                        get("/api/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]", is("'name' request parameter is required")));
    }

    @Test
    @DisplayName("GET /api/teams?name returns 200 when `name` is provided and pageable is default")
    public void getTeamsByName_NameProvidedWithDefaultPageable_StatusOk() throws Exception {
        var pValue = "test";
        var defaultPageNumber = 0;
        var defaultPageSize = 20;

        Page<TeamDto> expectedPage = Page.empty();

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
    @DisplayName("GET /api/teams?name returns 200 when `name` is provided and pageable values are custom")
    public void getTeamsByName_NameProvidedWithCustomPageParameters_StatusOk() throws Exception {
        var pValue = "test";
        var testPageSize = 7;
        var testPageNumber = 4;
        var expectedPageable = Pageable.ofSize(testPageSize).withPage(testPageNumber);
        var expectedContent = List.of(TeamDto.builder().name(pValue).build());

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
}
