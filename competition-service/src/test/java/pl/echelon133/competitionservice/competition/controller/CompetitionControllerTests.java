package pl.echelon133.competitionservice.competition.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ml.echelon133.common.competition.dto.CompetitionDto;
import ml.echelon133.common.exception.ResourceNotFoundException;
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
import pl.echelon133.competitionservice.competition.TestUpsertCompetitionDto;
import pl.echelon133.competitionservice.competition.TestUpsertGroupDto;
import pl.echelon133.competitionservice.competition.TestUpsertLegendDto;
import pl.echelon133.competitionservice.competition.exceptions.CompetitionInvalidException;
import pl.echelon133.competitionservice.competition.model.Competition;
import pl.echelon133.competitionservice.competition.model.UpsertCompetitionDto;
import pl.echelon133.competitionservice.competition.service.CompetitionService;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class CompetitionControllerTests {

    private MockMvc mvc;

    @Mock
    private CompetitionService competitionService;

    @InjectMocks
    private CompetitionExceptionHandler competitionExceptionHandler;

    @InjectMocks
    private CompetitionController competitionController;

    private JacksonTester<CompetitionDto> jsonCompetitionDto;

    private JacksonTester<UpsertCompetitionDto> jsonUpsertCompetitionDto;

    @BeforeEach
    public void beforeEach() {
        var om = new ObjectMapper();

        JacksonTester.initFields(this, om);
        mvc = MockMvcBuilders
                .standaloneSetup(competitionController)
                .setControllerAdvice(competitionExceptionHandler)
                .setCustomArgumentResolvers(
                        // required while testing controller methods which use Pageable
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    private List<String> generateTeamIds(int howMany) {
        return IntStream.range(0, howMany)
                .mapToObj(v -> UUID.randomUUID().toString())
                .collect(Collectors.toList());
    }

    private List<UpsertCompetitionDto.UpsertGroupDto> generateGroups(int howMany) {
        return IntStream.range(0, howMany)
                .mapToObj(v -> TestUpsertGroupDto.builder().build())
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("GET /api/competitions/:id returns 404 when resource not found")
    public void getCompetition_CompetitionNotFound_StatusNotFound() throws Exception {
        var competitionId = UUID.randomUUID();

        // given
        given(competitionService.findById(competitionId)).willThrow(
                new ResourceNotFoundException(Competition.class, competitionId)
        );

        // when
        mvc.perform(
                        get("/api/competitions/" + competitionId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("competition %s could not be found", competitionId)
                )));
    }

    @Test
    @DisplayName("GET /api/competitions/:id returns 200 and a valid entity if entity found")
    public void getCompetition_CompetitionFound_StatusOk() throws Exception {
        var competitionId = UUID.randomUUID();

        var competitionDto = CompetitionDto.from(competitionId, "test1", "test2", "test3");
        var expectedJson = jsonCompetitionDto.write(competitionDto).getJson();

        // given
        given(competitionService.findById(competitionId)).willReturn(competitionDto);

        // when
        mvc.perform(
                        get("/api/competitions/" + competitionId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("DELETE /api/competitions/:id returns 200 and a counter of how many competitions have been deleted")
    public void deleteCompetition_CompetitionIdProvided_StatusOkAndReturnsCounter() throws Exception {
        var id = UUID.randomUUID();

        // given
        given(competitionService.markCompetitionAsDeleted(id)).willReturn(1);

        // when
        mvc.perform(
                        delete("/api/competitions/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("{\"deleted\":1}"));
    }

    @Test
    @DisplayName("GET /api/competitions?name= returns 400 when `name` is not provided")
    public void getCompetitionsByName_NameNotProvided_StatusBadRequest() throws Exception {
        mvc.perform(
                        get("/api/competitions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]", is("query parameter 'name' not provided")));
    }

    @Test
    @DisplayName("GET /api/competitions?name returns 200 when `name` is provided and pageable is default")
    public void getCompetitionsByName_NameProvidedWithDefaultPageable_StatusOk() throws Exception {
        var pValue = "test";
        var defaultPageNumber = 0;
        var defaultPageSize = 20;

        Page<CompetitionDto> expectedPage = Page.empty();

        //given
        given(competitionService.findCompetitionsByName(
                eq(pValue),
                argThat(p -> p.getPageSize() == defaultPageSize && p.getPageNumber() == defaultPageNumber)
        )).willReturn(expectedPage);

        mvc.perform(
                        get("/api/competitions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("name", pValue)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(0)));
    }

    @Test
    @DisplayName("GET /api/competitions?name returns 200 when `name` is provided and pageable values are custom")
    public void getCompetitionsByName_NameProvidedWithCustomPageParameters_StatusOk() throws Exception {
        var pValue = "test";
        var testPageSize = 7;
        var testPageNumber = 4;
        var expectedPageable = Pageable.ofSize(testPageSize).withPage(testPageNumber);
        var expectedContent = List.of(CompetitionDto.from(UUID.randomUUID(), pValue, "test2", "test3"));

        Page<CompetitionDto> expectedPage = new PageImpl<>(expectedContent, expectedPageable, 1);

        //given
        given(competitionService.findCompetitionsByName(
                eq(pValue),
                argThat(p -> p.getPageNumber() == testPageNumber && p.getPageSize() == testPageSize)
        )).willReturn(expectedPage);

        mvc.perform(
                        get("/api/competitions")
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
    @DisplayName("POST /api/competitions returns 422 when name is not provided")
    public void createCompetition_NameNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertCompetitionDto.builder().name(null).build();
        var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/competitions")
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
    @DisplayName("POST /api/competitions returns 422 when name length is incorrect")
    public void createCompetition_NameLengthIncorrect_StatusUnprocessableEntity() throws Exception {
        var incorrectNameLengths = List.of(
                // too short (0 characters)
                "",
                /// too long (51 characters)
                "a".repeat(51)
        );

        for (String incorrectName : incorrectNameLengths) {
            var contentDto = TestUpsertCompetitionDto.builder().name(incorrectName).build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("name", List.of("expected length between 1 and 50")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when name length is correct")
    public void createCompetition_NameLengthCorrect_StatusOk() throws Exception {
        var correctNameLengths = List.of(
                // minimum 1 character
                "a",
                // maximum 50 characters
                "a".repeat(50)
        );

        // given
        given(competitionService.createCompetition(any())).willReturn(UUID.randomUUID());

        for (String correctName : correctNameLengths) {
            var contentDto = TestUpsertCompetitionDto.builder().name(correctName).build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            // when
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when season is not provided")
    public void createCompetition_SeasonNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertCompetitionDto.builder().season(null).build();
        var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/competitions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("season", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when season length is incorrect")
    public void createCompetition_SeasonLengthIncorrect_StatusUnprocessableEntity() throws Exception {
        var incorrectSeasonLengths = List.of(
                // too short (0 characters)
                "",
                /// too long (31 characters)
                "a".repeat(31)
        );

        for (String incorrectSeason : incorrectSeasonLengths) {
            var contentDto = TestUpsertCompetitionDto.builder().season(incorrectSeason).build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("season", List.of("expected length between 1 and 30")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when season length is correct")
    public void createCompetition_SeasonLengthCorrect_StatusOk() throws Exception {
        var correctSeasonLengths = List.of(
                // minimum 1 character
                "a",
                // maximum 30 characters
                "a".repeat(30)
        );

        // given
        given(competitionService.createCompetition(any())).willReturn(UUID.randomUUID());

        for (String correctSeason : correctSeasonLengths) {
            var contentDto = TestUpsertCompetitionDto.builder().season(correctSeason).build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            // when
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when logoUrl is not provided")
    public void createCompetition_LogoUrlNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertCompetitionDto.builder().logoUrl(null).build();
        var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/competitions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("logoUrl", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/teams returns 422 when logoUrl is not a valid url")
    public void createTeam_LogoUrlInvalidUrl_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertCompetitionDto.builder().logoUrl("http//aaaaaaaab").build();
        var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/competitions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("logoUrl", List.of("not a valid url")))
                );
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when logoUrl length is incorrect")
    public void createCompetition_LogoUrlLengthIncorrect_StatusUnprocessableEntity() throws Exception {
        var incorrectLogoUrlLengths = List.of(
                // too short (14 characters)
                "http://taa.com",
                // too long (501 characters (15 + 486))
                "http://taa.com/" + "a".repeat(486)
        );

        for (String incorrectLogoUrl : incorrectLogoUrlLengths) {
            var contentDto = TestUpsertCompetitionDto.builder().logoUrl(incorrectLogoUrl).build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("logoUrl", List.of("expected length between 15 and 500")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when logoUrl length is correct")
    public void createCompetition_LogoUrlLengthCorrect_StatusOk() throws Exception {
        var correctLogoUrlLengths = List.of(
                // min 15
                "http://aaaaa.eu",
                /// max 500 (500 characters (15 + 485))
                "http://aaaaa.eu" + "a".repeat(485)
        );

        // given
        given(competitionService.createCompetition(any())).willReturn(UUID.randomUUID());

        for (String correctLogoUrl : correctLogoUrlLengths) {
            var contentDto = TestUpsertCompetitionDto.builder().logoUrl(correctLogoUrl).build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            // when
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when the number of groups in a competition is incorrect")
    public void createCompetition_GroupsSizeIncorrect_StatusUnprocessableEntity() throws Exception {
        List<List<UpsertCompetitionDto.UpsertGroupDto>> incorrectGroupSizes = List.of(
                // 0 groups
                generateGroups(0),
                /// 11 groups
                generateGroups(11)
        );

        for (List<UpsertCompetitionDto.UpsertGroupDto> groups : incorrectGroupSizes) {
            var contentDto = TestUpsertCompetitionDto.builder().groups(groups).build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("groups", List.of("size must be between 1 and 10")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when the number of groups in a competition is correct")
    public void createCompetition_GroupsSizeCorrect_StatusOk() throws Exception {
        List<List<UpsertCompetitionDto.UpsertGroupDto>> incorrectGroupSizes = List.of(
                // 1 group
                generateGroups(1),
                /// 10 groups
                generateGroups(10)
        );

        // given
        given(competitionService.createCompetition(any())).willReturn(UUID.randomUUID());

        for (List<UpsertCompetitionDto.UpsertGroupDto> groups : incorrectGroupSizes) {
            var contentDto = TestUpsertCompetitionDto.builder().groups(groups).build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            // when
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when there are duplicate teams in groups")
    public void createCompetition_DuplicateTeamsInGroups_StatusUnprocessableEntity() throws Exception {
        var duplicateTeam = UUID.randomUUID().toString();
        var groupATeams = List.of(duplicateTeam, UUID.randomUUID().toString());
        var groupBTeams = List.of(duplicateTeam, UUID.randomUUID().toString());

        var groups = Stream.of(groupATeams, groupBTeams).map(teams ->
                TestUpsertGroupDto.builder().teams(teams).build()
        ).collect(Collectors.toList());

        var contentDto = TestUpsertCompetitionDto.builder().groups(groups).build();
        var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/competitions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("groups", List.of("team cannot be a member of multiple groups in one competition")))
                );
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when there are only unique teams in groups")
    public void createCompetition_OnlyUniqueTeamsInGroups_StatusOk() throws Exception {
        // create 10 groups of 10 unique teams each (100 unique teams)
        var groups = IntStream.range(0, 10)
                .mapToObj(i -> TestUpsertGroupDto.builder().teams(generateTeamIds(10)).build())
                .collect(Collectors.toList());

        var contentDto = TestUpsertCompetitionDto.builder().groups(groups).build();
        var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

        // given
        given(competitionService.createCompetition(any())).willReturn(UUID.randomUUID());

        // when
        mvc.perform(
                        post("/api/competitions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when group's name is not provided")
    public void createCompetition_GroupNameNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertCompetitionDto.builder()
                .groups(List.of(TestUpsertGroupDto.builder().name(null).build()))
                .build();
        var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/competitions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("groups[0].name", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when group's name length is incorrect")
    public void createCompetition_GroupNameLengthIncorrect_StatusUnprocessableEntity() throws Exception {
        // too long (51 characters)
        var incorrectName = "a".repeat(51);

        var contentDto = TestUpsertCompetitionDto.builder()
                .groups(List.of(TestUpsertGroupDto.builder().name(incorrectName).build()))
                .build();
        var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/competitions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("groups[0].name", List.of("expected length between 0 and 50")))
                );
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when group's name length is correct")
    public void createCompetition_GroupNameLengthCorrect_StatusOk() throws Exception {
        var correctName = "a".repeat(50);

        var contentDto = TestUpsertCompetitionDto.builder()
                .groups(List.of(TestUpsertGroupDto.builder().name(correctName).build()))
                .build();
        var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

        // given
        given(competitionService.createCompetition(any())).willReturn(UUID.randomUUID());

        // when
        mvc.perform(
                        post("/api/competitions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when the number of teams in a group is incorrect")
    public void createCompetition_TeamSizeInGroupIncorrect_StatusUnprocessableEntity() throws Exception {
        List<List<String>> incorrectGroupTeamSizes = List.of(
                // 1 team
                generateTeamIds(1),
                /// 25 teams
                generateTeamIds(25)
        );

        for (List<String> teams : incorrectGroupTeamSizes) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .groups(List.of(TestUpsertGroupDto.builder().teams(teams).build()))
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("groups[0].teams", List.of("size must be between 2 and 24")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when the number of teams in a group is correct")
    public void createCompetition_TeamSizeInGroupCorrect_StatusOk() throws Exception {
        List<List<String>> correctGroupTeamSizes = List.of(
                // 2 teams
                generateTeamIds(2),
                /// 24 teams
                generateTeamIds(24)
        );

        // given
        given(competitionService.createCompetition(any())).willReturn(UUID.randomUUID());

        for (List<String> teams : correctGroupTeamSizes) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .groups(List.of(TestUpsertGroupDto.builder().teams(teams).build()))
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            // when
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when team's id in a group is not a uuid")
    public void createCompetition_TeamIdInGroupIncorrect_StatusUnprocessableEntity() throws Exception {
        List<String> incorrectTeamIds = List.of(
                UUID.randomUUID() + "asdf",
                "abcd",
                "a".repeat(50)
        );

        for (String incorrectTeamId : incorrectTeamIds) {
            // mix correct and incorrect ids
            var teams = List.of(UUID.randomUUID().toString(), incorrectTeamId);
            var contentDto = TestUpsertCompetitionDto.builder()
                    .groups(List.of(TestUpsertGroupDto.builder().teams(teams).build()))
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("groups[0].teams[1]", List.of("not a valid uuid")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when the number of legend entries in a competition is incorrect")
    public void createCompetition_LegendSizeIncorrect_StatusUnprocessableEntity() throws Exception {
        // 7 legend entries, one for each position in the table
        List<UpsertCompetitionDto.UpsertLegendDto> incorrectLegendSize = IntStream.range(1, 8)
                .mapToObj(i -> TestUpsertLegendDto.builder().positions(Set.of(i)).build())
                .collect(Collectors.toList());

        var contentDto = TestUpsertCompetitionDto.builder()
                .legend(incorrectLegendSize)
                .build();
        var json = jsonUpsertCompetitionDto.write(contentDto).getJson();
        mvc.perform(
                        post("/api/competitions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("legend", List.of("size must be between 0 and 6")))
                );
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when the number of legend entries in a competition is correct")
    public void createCompetition_LegendSizeCorrect_StatusOk() throws Exception {
        List<List<UpsertCompetitionDto.UpsertLegendDto>> correctLegendSize = List.of(
                // empty
                List.of(),
                // 6 legend entries, one for each position in the table
                IntStream.range(1, 7)
                        .mapToObj(i -> TestUpsertLegendDto.builder().positions(Set.of(i)).build())
                        .collect(Collectors.toList())
        );

        // given
        given(competitionService.createCompetition(any())).willReturn(UUID.randomUUID());

        for (List<UpsertCompetitionDto.UpsertLegendDto> legend : correctLegendSize) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .legend(legend)
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            // when
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when there are duplicate positions in legend entries")
    public void createCompetition_DuplicatePositionsInLegend_StatusUnprocessableEntity() throws Exception {
        List<List<UpsertCompetitionDto.UpsertLegendDto>> duplicatePositionLegend = List.of(
                // first entry for (1, 2), second for (2, 3), which duplicates 2
                List.of(Set.of(1, 2), Set.of(2, 3)).stream().map(posSet ->
                        TestUpsertLegendDto.builder().positions(posSet).build()).collect(Collectors.toList()),
                // first entry for (1, 2), second for (3, 4), third for (4, 5, 6), which duplicates 4
                List.of(Set.of(1, 2), Set.of(3, 4), Set.of(4, 5, 6)).stream().map(posSet ->
                        TestUpsertLegendDto.builder().positions(posSet).build()).collect(Collectors.toList())
        );

        for (List<UpsertCompetitionDto.UpsertLegendDto> legend : duplicatePositionLegend) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .legend(legend)
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("legend", List.of("multiple legend entries cannot reference the same position")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when the number of positions of a legend is incorrect")
    public void createCompetition_LegendPositionSizeIncorrect_StatusUnprocessableEntity() throws Exception {
        List<List<UpsertCompetitionDto.UpsertLegendDto>> incorrectPositionSize = List.of(
                // 0 positions
                List.of(TestUpsertLegendDto.builder().positions(Set.of()).build()),
                // 7 positions
                List.of(TestUpsertLegendDto.builder().positions(Set.of(1, 2, 3, 4, 5, 6, 7)).build())
        );

        for (List<UpsertCompetitionDto.UpsertLegendDto> legend : incorrectPositionSize) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .legend(legend)
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("legend[0].positions", List.of("size must be between 1 and 6")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when the number of positions of a legend is correct")
    public void createCompetition_LegendPositionSizeCorrect_StatusOk() throws Exception {
        List<List<UpsertCompetitionDto.UpsertLegendDto>> correctPositionSize = List.of(
                // 1 position
                List.of(TestUpsertLegendDto.builder().positions(Set.of(1)).build()),
                // 6 positions
                List.of(TestUpsertLegendDto.builder().positions(Set.of(2, 3, 4, 5, 6, 7)).build())
        );

        // given
        given(competitionService.createCompetition(any())).willReturn(UUID.randomUUID());

        for (List<UpsertCompetitionDto.UpsertLegendDto> legend : correctPositionSize) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .legend(legend)
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            // when
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when legend's context is not provided")
    public void createCompetition_LegendContextNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertCompetitionDto.builder()
                .legend(List.of(TestUpsertLegendDto.builder().context(null).build()))
                .build();
        var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/competitions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("legend[0].context", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when legend's context length is incorrect")
    public void createCompetition_LegendContextLengthIncorrect_StatusUnprocessableEntity() throws Exception {
        var incorrectContextLengths = List.of(
                // too short (0)
                "",
                // too long (201)
                "a".repeat(201)
        );

        for (String context : incorrectContextLengths) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .legend(List.of(TestUpsertLegendDto.builder().context(context).build()))
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("legend[0].context", List.of("expected length between 1 and 200")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when legend's context length is correct")
    public void createCompetition_LegendContextLengthCorrect_StatusOk() throws Exception {
        var correctContextLengths = List.of(
                // min (1)
                "a",
                // max (200)
                "a".repeat(200)
        );

        // given
        given(competitionService.createCompetition(any())).willReturn(UUID.randomUUID());

        for (String context : correctContextLengths) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .legend(List.of(TestUpsertLegendDto.builder().context(context).build()))
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            // when
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when legend's sentiment is not provided")
    public void createCompetition_LegendSentimentNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertCompetitionDto.builder()
                .legend(List.of(TestUpsertLegendDto.builder().sentiment(null).build()))
                .build();
        var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/competitions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("legend[0].sentiment", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when legend's sentiment value is incorrect")
    public void createCompetition_LegendSentimentValueIncorrect_StatusUnprocessableEntity() throws Exception {
        var incorrectSentiments = List.of(
                "asdf", "positive_ASDF", "negative__a", "POSITIVEA", "positivec", "negative__c"
        );

        for (String sentiment : incorrectSentiments) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .legend(List.of(TestUpsertLegendDto.builder().sentiment(sentiment).build()))
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("legend[0].sentiment", List.of("required exactly one of [POSITIVE_A, POSITIVE_B, POSITIVE_C, POSITIVE_D, NEGATIVE_A, NEGATIVE_B]")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when legend's sentiment value is correct")
    public void createCompetition_LegendSentimentValueCorrect_StatusOk() throws Exception {
        var correctSentiments = List.of(
                "POSITIVE_A", "POSITIVE_B", "POSITIVE_C", "POSITIVE_D", "NEGATIVE_A", "NEGATIVE_B", // uppercase
                "positive_a", "positive_b", "positive_c", "positive_d", "negative_a", "negative_b", // lowercase
                "POSitive_A", "positive_B", "POSITIVE_c", "posITive_D", "negATIVE_A", "NEGATIVE_B" // mixed
        );

        // given
        given(competitionService.createCompetition(any())).willReturn(UUID.randomUUID());

        for (String sentiment : correctSentiments) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .legend(List.of(TestUpsertLegendDto.builder().sentiment(sentiment).build()))
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            // when
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when legend references positions that do not appear in a group")
    public void createCompetition_LegendPositionsNotInRangeForSingleGroup_StatusUnprocessableEntity() throws Exception {
        // default TestUpsertGroupDto in TestUpsertCompetitionDto creates a single group of 10 teams,
        // which means that positions 0, 11, 12, 13, etc. are not legal, because they refer to
        // positions which do not exist in a group
        List<UpsertCompetitionDto.UpsertLegendDto> incorrectPositionReferences =
                IntStream
                        .of(0, 11, 20, 25, 50)
                        .mapToObj(i -> TestUpsertLegendDto.builder().positions(Set.of(i)).build())
                        .collect(Collectors.toList());

        for (UpsertCompetitionDto.UpsertLegendDto legend : incorrectPositionReferences) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .legend(List.of(legend))
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("general", List.of("legend cannot reference positions which do not exist in groups"))
                    )
            );
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when legend references positions that appear in a group")
    public void createCompetition_LegendPositionsInRangeForSingleGroup_StatusOk() throws Exception {
        // default TestUpsertGroupDto in TestUpsertCompetitionDto creates a single group of 10 teams,
        // which means that positions 1 through 10 are legal
        List<UpsertCompetitionDto.UpsertLegendDto> correctPositionReferences =
                IntStream
                        .range(1, 11)
                        .mapToObj(i -> TestUpsertLegendDto.builder().positions(Set.of(i)).build())
                        .collect(Collectors.toList());

        // given
        given(competitionService.createCompetition(any())).willReturn(UUID.randomUUID());

        for (UpsertCompetitionDto.UpsertLegendDto legend : correctPositionReferences) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .legend(List.of(legend))
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            // when
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when legend references positions that do not appear in at least one of many groups")
    public void createCompetition_LegendPositionsNotInRangeForMultipleGroups_StatusUnprocessableEntity() throws Exception {
        // create 8 groups, one with 2, one with 3, then 4, 5, 6, 7, 8, 9 teams each
        List<UpsertCompetitionDto.UpsertGroupDto> groups = IntStream.range(2, 10).mapToObj(teamCount ->
                TestUpsertGroupDto.builder().teams(generateTeamIds(teamCount)).build()
        ).collect(Collectors.toList());

        // there are 8 groups with at most 9 teams, therefore it should NOT be legal to refer to positions
        // 0, and then from 10th going on
        List<UpsertCompetitionDto.UpsertLegendDto> incorrectPositionReferences =
                IntStream
                        .of(0, 10, 11, 20, 24)
                        .mapToObj(i -> TestUpsertLegendDto.builder().positions(Set.of(i)).build())
                        .collect(Collectors.toList());

        for (UpsertCompetitionDto.UpsertLegendDto legend : incorrectPositionReferences) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .groups(groups)
                    .legend(List.of(legend))
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("general", List.of("legend cannot reference positions which do not exist in groups")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when legend references positions that appear in at least one of many groups")
    public void createCompetition_LegendPositionsInRangeForMultipleGroups_StatusOk() throws Exception {
        // create 8 groups, one with 2, one with 3, then 4, 5, 6, 7, 8, 9 teams each
        List<UpsertCompetitionDto.UpsertGroupDto> groups = IntStream.range(2, 10).mapToObj(teamCount ->
                TestUpsertGroupDto.builder().teams(generateTeamIds(teamCount)).build()
        ).collect(Collectors.toList());

        // there are 8 groups with at most 9 teams, therefore it should be legal to refer to positions
        // 1 through 9
        List<UpsertCompetitionDto.UpsertLegendDto> correctPositionReferences =
                IntStream
                        .range(1, 10)
                        .mapToObj(i -> TestUpsertLegendDto.builder().positions(Set.of(i)).build())
                        .collect(Collectors.toList());

        // given
        given(competitionService.createCompetition(any())).willReturn(UUID.randomUUID());

        for (UpsertCompetitionDto.UpsertLegendDto legend : correctPositionReferences) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .groups(groups)
                    .legend(List.of(legend))
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            // when
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when the service method throws")
    public void createCompetition_ServiceThrows_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertCompetitionDto.builder().build();
        var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

        var teamIdToFail = UUID.fromString(contentDto.getGroups().get(0).getTeams().get(0));

        // given
        given(competitionService.createCompetition(
                argThat(c -> c.getGroups().get(0).getTeams().contains(teamIdToFail.toString()))
        )).willThrow(new CompetitionInvalidException("failed to fetch resource with id " + teamIdToFail));

        // then
        mvc.perform(
                        post("/api/competitions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages[0]", is("failed to fetch resource with id " + teamIdToFail))
                );
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when all data in the body is correct and service saves the entity")
    public void createCompetition_ServiceSaves_StatusOk() throws Exception {
        var contentDto = TestUpsertCompetitionDto.builder().build();
        var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

        var competitionId = UUID.randomUUID();

        // given
        given(competitionService.createCompetition(argThat(c -> c.getName().equals(contentDto.getName()))))
                .willReturn(competitionId);

        var expectedJson = String.format("{\"id\":\"%s\"}", competitionId);

        // then
        mvc.perform(
                        post("/api/competitions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }
}
