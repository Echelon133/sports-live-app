package pl.echelon133.competitionservice.competition.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import pl.echelon133.competitionservice.competition.TestUpsertLeaguePhaseDto;
import pl.echelon133.competitionservice.competition.TestUpsertLegendDto;
import pl.echelon133.competitionservice.competition.exceptions.CompetitionInvalidException;
import pl.echelon133.competitionservice.competition.exceptions.CompetitionPhaseNotFoundException;
import pl.echelon133.competitionservice.competition.exceptions.CompetitionRoundNotEmptyException;
import pl.echelon133.competitionservice.competition.exceptions.CompetitionRoundNotFoundException;
import pl.echelon133.competitionservice.competition.model.*;
import pl.echelon133.competitionservice.competition.service.CompetitionService;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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

    private JacksonTester<StandingsDto> jsonStandingsDto;

    private JacksonTester<UpsertRoundDto> jsonUpsertRoundDto;

    private JacksonTester<UpsertKnockoutTreeDto> jsonUpsertKnockoutTreeDto;

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

        var competitionDto = CompetitionDto.from(competitionId, "test1", "test2", "test3", true, true);
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
    @DisplayName("GET /api/competitions/:id/matches/unassigned returns 200 when competitionId is provided and pageable is default")
    public void getUnassignedMatches_ProvidedCompetitionIdWithDefaultPageable_StatusOk() throws Exception {
        var competitionId = UUID.randomUUID();
        var defaultPageSize = 20;
        var defaultPageNumber = 0;
        var expectedPageable = Pageable.ofSize(defaultPageSize).withPage(defaultPageNumber);
        var expectedPage = new PageImpl<CompactMatchDto>(List.of(), expectedPageable, 0);

        // given
        given(competitionService.findUnassignedMatches(
                eq(competitionId),
                argThat(p -> p.getPageSize() == defaultPageSize && p.getPageNumber() == defaultPageNumber)
        )).willReturn(expectedPage);

        // when
        mvc.perform(
                        get("/api/competitions/" + competitionId + "/matches/unassigned")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(0)));
    }

    @Test
    @DisplayName("GET /api/competitions/:id/matches/unassigned returns 200 when competitionId is provided and pageable is custom")
    public void getUnassignedMatches_ProvidedCompetitionIdWithCustomPageable_StatusOk() throws Exception {
        var competitionId = UUID.randomUUID();
        var testPageSize = 25;
        var testPageNumber = 5;
        var expectedPageable = Pageable.ofSize(testPageSize).withPage(testPageNumber);
        var expectedPage = new PageImpl<CompactMatchDto>(List.of(), expectedPageable, 0);

        // given
        given(competitionService.findUnassignedMatches(
                eq(competitionId),
                argThat(p -> p.getPageSize() == testPageSize && p.getPageNumber() == testPageNumber)
        )).willReturn(expectedPage);

        // when
        mvc.perform(
                        get("/api/competitions/" + competitionId + "/matches/unassigned")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("size", String.valueOf(testPageSize))
                                .param("page", String.valueOf(testPageNumber))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(0)));
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
        var expectedPageable = Pageable.ofSize(defaultPageSize).withPage(defaultPageNumber);

        Page<CompetitionDto> expectedPage = Page.empty(expectedPageable);

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
        var expectedContent = List.of(CompetitionDto.from(UUID.randomUUID(), pValue, "test2", "test3", true, true));

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
    @DisplayName("GET /api/competitions/pinned returns 200")
    public void getPinnedCompetitions_NoRequestParameters_StatusOk() throws Exception {
        var expectedName = "Competition 1";
        var expectedContent = List.of(CompetitionDto.from(UUID.randomUUID(), expectedName, "test2", "test3", true, true));

        // given
        given(competitionService.findPinnedCompetitions()).willReturn(expectedContent);

        // when
        mvc.perform(
                        get("/api/competitions/pinned")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()", is(1)))
                .andExpect(jsonPath("$.[0].name", is(expectedName)));
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when neither league phase nor knockout phase is provided")
    public void createCompetition_BothPhasesNull_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertCompetitionDto.builder()
                .leaguePhase(null)
                .knockoutPhase(null)
                .build();
        var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

        // then
        mvc.perform(
                        post("/api/competitions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("general", List.of("a competition must have at least one phase")))
                );
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
            var contentDto = TestUpsertCompetitionDto.builder()
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().build())
                    .name(correctName)
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
            var contentDto = TestUpsertCompetitionDto.builder()
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().build())
                    .season(correctSeason)
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
            var contentDto = TestUpsertCompetitionDto.builder()
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().build())
                    .logoUrl(correctLogoUrl)
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
    @DisplayName("POST /api/competitions returns 422 when the number of groups in a competition is incorrect")
    public void createCompetition_GroupsSizeIncorrect_StatusUnprocessableEntity() throws Exception {
        List<List<UpsertCompetitionDto.UpsertGroupDto>> incorrectGroupSizes = List.of(
                // 0 groups
                generateGroups(0),
                /// 11 groups
                generateGroups(11)
        );

        for (List<UpsertCompetitionDto.UpsertGroupDto> groups : incorrectGroupSizes) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().groups(groups).build())
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
                            jsonPath("$.messages", hasEntry("leaguePhase.groups", List.of("size must be between 1 and 10")))
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
            var contentDto = TestUpsertCompetitionDto.builder()
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().groups(groups).build())
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
    @DisplayName("POST /api/competitions returns 422 when there are duplicate teams in groups")
    public void createCompetition_DuplicateTeamsInGroups_StatusUnprocessableEntity() throws Exception {
        var duplicateTeam = UUID.randomUUID().toString();
        var groupATeams = List.of(duplicateTeam, UUID.randomUUID().toString());
        var groupBTeams = List.of(duplicateTeam, UUID.randomUUID().toString());

        var groups = Stream.of(groupATeams, groupBTeams).map(teams ->
                TestUpsertGroupDto.builder().teams(teams).build()
        ).collect(Collectors.toList());

        var contentDto = TestUpsertCompetitionDto.builder()
                .leaguePhase(TestUpsertLeaguePhaseDto.builder().groups(groups).build())
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
                        jsonPath("$.messages", hasEntry("leaguePhase.groups", List.of("team cannot be a member of multiple groups in one competition")))
                );
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when there are only unique teams in groups")
    public void createCompetition_OnlyUniqueTeamsInGroups_StatusOk() throws Exception {
        // create 10 groups of 10 unique teams each (100 unique teams)
        var groups = IntStream.range(0, 10)
                .mapToObj(i -> TestUpsertGroupDto.builder().teams(generateTeamIds(10)).build())
                .collect(Collectors.toList());

        var contentDto = TestUpsertCompetitionDto.builder()
                .leaguePhase(TestUpsertLeaguePhaseDto.builder().groups(groups).build())
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
    @DisplayName("POST /api/competitions returns 422 when group's name is not provided")
    public void createCompetition_GroupNameNotProvided_StatusUnprocessableEntity() throws Exception {
        var noGroupNameGroups = List.of(TestUpsertGroupDto.builder().name(null).build());
        var contentDto = TestUpsertCompetitionDto.builder()
                .leaguePhase(TestUpsertLeaguePhaseDto.builder().groups(noGroupNameGroups).build())
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
                        jsonPath("$.messages", hasEntry("leaguePhase.groups[0].name", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when group's name length is incorrect")
    public void createCompetition_GroupNameLengthIncorrect_StatusUnprocessableEntity() throws Exception {
        // too long (51 characters)
        var incorrectName = "a".repeat(51);

        var incorrectNameGroups = List.of(TestUpsertGroupDto.builder().name(incorrectName).build());
        var contentDto = TestUpsertCompetitionDto.builder()
                .leaguePhase(TestUpsertLeaguePhaseDto.builder().groups(incorrectNameGroups).build())
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
                        jsonPath("$.messages", hasEntry("leaguePhase.groups[0].name", List.of("expected length between 0 and 50")))
                );
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when group's name length is correct")
    public void createCompetition_GroupNameLengthCorrect_StatusOk() throws Exception {
        var correctName = "a".repeat(50);

        var correctNameGroups = List.of(TestUpsertGroupDto.builder().name(correctName).build());
        var contentDto = TestUpsertCompetitionDto.builder()
                .leaguePhase(TestUpsertLeaguePhaseDto.builder().groups(correctNameGroups).build())
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
                /// 37 teams
                generateTeamIds(37)
        );

        for (List<String> teams : incorrectGroupTeamSizes) {
            var groupWithTeams = List.of(TestUpsertGroupDto.builder().teams(teams).build());
            var contentDto = TestUpsertCompetitionDto.builder()
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().groups(groupWithTeams).build())
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
                            jsonPath("$.messages", hasEntry("leaguePhase.groups[0].teams", List.of("size must be between 2 and 36")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when the number of teams in a group is correct")
    public void createCompetition_TeamSizeInGroupCorrect_StatusOk() throws Exception {
        List<List<String>> correctGroupTeamSizes = List.of(
                // 2 teams
                generateTeamIds(2),
                /// 36 teams
                generateTeamIds(36)
        );

        // given
        given(competitionService.createCompetition(any())).willReturn(UUID.randomUUID());

        for (List<String> teams : correctGroupTeamSizes) {
            var correctSizeGroups = List.of(TestUpsertGroupDto.builder().teams(teams).build());
            var contentDto = TestUpsertCompetitionDto.builder()
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().groups(correctSizeGroups).build())
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
            var mixedGroups = List.of(TestUpsertGroupDto.builder().teams(teams).build());
            var contentDto = TestUpsertCompetitionDto.builder()
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().groups(mixedGroups).build())
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
                            jsonPath("$.messages", hasEntry("leaguePhase.groups[0].teams[1]", List.of("not a valid uuid")))
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
                .leaguePhase(TestUpsertLeaguePhaseDto.builder().legend(incorrectLegendSize).build())
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
                        jsonPath("$.messages", hasEntry("leaguePhase.legend", List.of("size must be between 0 and 6")))
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
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().legend(legend).build())
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
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().legend(legend).build())
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
                            jsonPath("$.messages", hasEntry("leaguePhase.legend", List.of("multiple legend entries cannot reference the same position")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when the number of positions of a legend is incorrect")
    public void createCompetition_LegendPositionSizeIncorrect_StatusUnprocessableEntity() throws Exception {
        List<List<UpsertCompetitionDto.UpsertLegendDto>> incorrectPositionSize = List.of(
                // 0 positions
                List.of(TestUpsertLegendDto.builder().positions(Set.of()).build()),
                // 17 positions
                List.of(TestUpsertLegendDto.builder().positions(
                        Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17)
                ).build()
                ));

        for (List<UpsertCompetitionDto.UpsertLegendDto> legend : incorrectPositionSize) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().legend(legend).build())
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
                            jsonPath("$.messages", hasEntry("leaguePhase.legend[0].positions", List.of("size must be between 1 and 16")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when the number of positions of a legend is correct")
    public void createCompetition_LegendPositionSizeCorrect_StatusOk() throws Exception {
        List<List<UpsertCompetitionDto.UpsertLegendDto>> correctPositionSize = List.of(
                // 1 position
                List.of(TestUpsertLegendDto.builder().positions(Set.of(1)).build()),
                // 16 positions
                List.of(TestUpsertLegendDto.builder().positions(
                        Set.of(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17)
                ).build())
        );

        // given
        given(competitionService.createCompetition(any())).willReturn(UUID.randomUUID());

        for (List<UpsertCompetitionDto.UpsertLegendDto> legend : correctPositionSize) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().legend(legend).build())
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            // when
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when legend's context is not provided")
    public void createCompetition_LegendContextNotProvided_StatusUnprocessableEntity() throws Exception {
        var legend = List.of(TestUpsertLegendDto.builder().context(null).build());
        var contentDto = TestUpsertCompetitionDto.builder()
                .leaguePhase(TestUpsertLeaguePhaseDto.builder().legend(legend).build())
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
                        jsonPath("$.messages", hasEntry("leaguePhase.legend[0].context", List.of("field has to be provided")))
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
            var legend = List.of(TestUpsertLegendDto.builder().context(context).build());
            var contentDto = TestUpsertCompetitionDto.builder()
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().legend(legend).build())
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
                            jsonPath("$.messages", hasEntry("leaguePhase.legend[0].context", List.of("expected length between 1 and 200")))
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
            var legend = List.of(TestUpsertLegendDto.builder().context(context).build());
            var contentDto = TestUpsertCompetitionDto.builder()
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().legend(legend).build())
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
        var legend = List.of(TestUpsertLegendDto.builder().sentiment(null).build());
        var contentDto = TestUpsertCompetitionDto.builder()
                .leaguePhase(TestUpsertLeaguePhaseDto.builder().legend(legend).build())
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
                        jsonPath("$.messages", hasEntry("leaguePhase.legend[0].sentiment", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/competitions returns 422 when legend's sentiment value is incorrect")
    public void createCompetition_LegendSentimentValueIncorrect_StatusUnprocessableEntity() throws Exception {
        var incorrectSentiments = List.of(
                "asdf", "positive_ASDF", "negative__a", "POSITIVEA", "positivec", "negative__c"
        );

        for (String sentiment : incorrectSentiments) {
            var legend = List.of(TestUpsertLegendDto.builder().sentiment(sentiment).build());
            var contentDto = TestUpsertCompetitionDto.builder()
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().legend(legend).build())
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
                            jsonPath("$.messages", hasEntry("leaguePhase.legend[0].sentiment", List.of("required exactly one of [POSITIVE_A, POSITIVE_B, POSITIVE_C, POSITIVE_D, NEGATIVE_A, NEGATIVE_B]")))
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
            var legend = List.of(TestUpsertLegendDto.builder().sentiment(sentiment).build());
            var contentDto = TestUpsertCompetitionDto.builder()
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().legend(legend).build())
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
        // default TestUpsertGroupDto creates a single group of 20 teams,
        // which means that positions 0, 21, 22, 23, etc. are not legal, because they refer to
        // positions which do not exist in a group
        List<UpsertCompetitionDto.UpsertLegendDto> incorrectPositionReferences =
                IntStream
                        .of(0, 21, 30, 35, 50)
                        .mapToObj(i -> TestUpsertLegendDto.builder().positions(Set.of(i)).build())
                        .collect(Collectors.toList());

        for (UpsertCompetitionDto.UpsertLegendDto legend : incorrectPositionReferences) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().legend(List.of(legend)).build())
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
                            jsonPath("$.messages", hasEntry("leaguePhase", List.of("legend cannot reference positions which do not exist in groups"))
                    )
            );
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when legend references positions that appear in a group")
    public void createCompetition_LegendPositionsInRangeForSingleGroup_StatusOk() throws Exception {
        // default TestUpsertGroupDto creates a single group of 20 teams,
        // which means that positions 1 through 20 are legal
        List<UpsertCompetitionDto.UpsertLegendDto> correctPositionReferences =
                IntStream
                        .range(1, 20)
                        .mapToObj(i -> TestUpsertLegendDto.builder().positions(Set.of(i)).build())
                        .collect(Collectors.toList());

        // given
        given(competitionService.createCompetition(any())).willReturn(UUID.randomUUID());

        for (UpsertCompetitionDto.UpsertLegendDto legend : correctPositionReferences) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().legend(List.of(legend)).build())
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
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().groups(groups).legend(List.of(legend)).build())
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
                            jsonPath("$.messages", hasEntry("leaguePhase", List.of("legend cannot reference positions which do not exist in groups")))
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
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().groups(groups).legend(List.of(legend)).build())
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
        var contentDto = TestUpsertCompetitionDto.builder()
                .leaguePhase(TestUpsertLeaguePhaseDto.builder().build())
                .build();
        var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

        var teamIdToFail = UUID.fromString(contentDto.leaguePhase().groups().get(0).teams().get(0));

        // given
        given(competitionService.createCompetition(
                argThat(c -> c.leaguePhase().groups().get(0).teams().contains(teamIdToFail.toString()))
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
    @DisplayName("POST /api/competitions returns 422 when the maximum number of rounds of the league phase is incorrect")
    public void createCompetition_IncorrectNumberOfMaximumRounds_StatusUnprocessableEntity() throws Exception {
        var incorrectMaxRounds = List.of(0, 51, 100, 200);

        for (var maxRounds : incorrectMaxRounds) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().maxRounds(maxRounds).build())
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            // then
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("leaguePhase.maxRounds", List.of("expected between 1 and 50 rounds in the league phase")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when the maximum number of rounds of the league phase is correct")
    public void createCompetition_CorrectNumberOfMaximumRounds_StatusOk() throws Exception {
        var correctMaxRounds = List.of(1, 5, 25, 50);

        // given
        given(competitionService.createCompetition(any())).willReturn(UUID.randomUUID());

        for (var maxRounds : correctMaxRounds) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .leaguePhase(TestUpsertLeaguePhaseDto.builder().maxRounds(maxRounds).build())
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            // then
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
    @DisplayName("POST /api/competitions returns 422 when knockout phase information is incorrect")
    public void createCompetition_IncorrectKnockoutPhaseInformation_StatusUnprocessableEntity() throws Exception {
        var incorrectStartsAt = List.of(
                "ROUND_OF_256", "ASFSDFS", "tesasdf", "", "roundof64", "semifinal"
        );

        var expectedMessage =
                "require exactly one of [ROUND_OF_128, ROUND_OF_64, ROUND_OF_32, ROUND_OF_16, QUARTER_FINAL, SEMI_FINAL, FINAL]";

        for (var startsAt : incorrectStartsAt) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .knockoutPhase(new UpsertCompetitionDto.UpsertKnockoutPhaseDto(startsAt))
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();

            // then
            mvc.perform(
                            post("/api/competitions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("knockoutPhase.startsAt", List.of(expectedMessage)))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/competitions returns 200 when knockout phase information is correct")
    public void createCompetition_CorrectKnockoutPhaseInformation_StatusOk() throws Exception {
        var correctStartsAt = List.of(
                "ROUND_OF_128", "round_of_64", "ROUND_of_32", "ROUND_OF_16",
                "QUARTER_final", "semi_FINAL", "FiNaL"
        );

        for (var startsAt : correctStartsAt) {
            var contentDto = TestUpsertCompetitionDto.builder()
                    .knockoutPhase(new UpsertCompetitionDto.UpsertKnockoutPhaseDto(startsAt))
                    .build();
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();
            var competitionId = UUID.randomUUID();

            // given
            doReturn(competitionId).when(competitionService).createCompetition(argThat(c -> c.name().equals(contentDto.name())));
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

    @Test
    @DisplayName("POST /api/competitions returns 200 for all combinations of phases")
    public void createCompetition_ServiceSaves_StatusOk() throws Exception {
        var competitions = List.of(
                // only a league phase
                TestUpsertCompetitionDto.builder()
                        .leaguePhase(TestUpsertLeaguePhaseDto.builder().build())
                        .build(),
                // only a knockout phase
                TestUpsertCompetitionDto.builder()
                        .knockoutPhase(new UpsertCompetitionDto.UpsertKnockoutPhaseDto(KnockoutStage.ROUND_OF_64.name()))
                        .build(),
                // both phases
                TestUpsertCompetitionDto.builder()
                        .leaguePhase(TestUpsertLeaguePhaseDto.builder().build())
                        .knockoutPhase(new UpsertCompetitionDto.UpsertKnockoutPhaseDto(KnockoutStage.ROUND_OF_64.name()))
                        .build()
        );

        for (var contentDto : competitions) {
            var json = jsonUpsertCompetitionDto.write(contentDto).getJson();
            var competitionId = UUID.randomUUID();

            // given
            doReturn(competitionId).when(competitionService).createCompetition(argThat(c -> c.name().equals(contentDto.name())));
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

    @Test
    @DisplayName("GET /api/competitions/:id/standings returns 404 when competition is not found")
    public void getStandings_CompetitionNotFound_StatusNotFound() throws Exception {
        var competitionId = UUID.randomUUID();

        // given
        given(competitionService.findStandings(competitionId)).willThrow(
                new ResourceNotFoundException(Competition.class, competitionId)
        );

        // when
        mvc.perform(
                        get("/api/competitions/" + competitionId + "/standings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("competition %s could not be found", competitionId)
                )));
    }

    @Test
    @DisplayName("GET /api/competitions/:id/standings returns 200 when competition is found")
    public void getStandings_CompetitionFound_StatusOk() throws Exception {
        var competitionId = UUID.randomUUID();

        var teamDto = new StandingsDto.TeamStatsDto(UUID.randomUUID(), "Test Team", "test-url");
        var groupDto = new StandingsDto.GroupDto("Group A", List.of(teamDto));
        var legendDto = new StandingsDto.LegendDto(Set.of(1), "Promotion", "POSITIVE_A");
        var standingsDto = new StandingsDto(List.of(groupDto), List.of(legendDto));

        var expectedJson = jsonStandingsDto.write(standingsDto).getJson();

        // given
        given(competitionService.findStandings(competitionId)).willReturn(standingsDto);

        // when
        mvc.perform(
                        get("/api/competitions/" + competitionId + "/standings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("GET /api/competitions/:id/player-stats returns 200 when competitionId is provided and pageable is default")
    public void getPlayerStats_CompetitionIdProvidedWithDefaultPageable_StatusOk() throws Exception {
        var competitionId = UUID.randomUUID();
        var defaultPageNumber = 0;
        var defaultPageSize = 25;
        var expectedPageable = Pageable.ofSize(defaultPageSize).withPage(defaultPageNumber);

        Page<PlayerStatsDto> expectedPage = Page.empty(expectedPageable);

        // given
        given(competitionService.findPlayerStatsByCompetition(
                eq(competitionId),
                argThat(p -> p.getPageSize() == defaultPageSize && p.getPageNumber() == defaultPageNumber)
        )).willReturn(expectedPage);

        // when
        mvc.perform(
                        get("/api/competitions/" + competitionId + "/player-stats")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(0)));
    }

    @Test
    @DisplayName("GET /api/competitions/:id/player-stats returns 200 when competitionId is provided and pageable is custom")
    public void getPlayerStats_CompetitionIdProvidedWithCustomPageParameters_StatusOk() throws Exception {
        var competitionId = UUID.randomUUID();
        var testPlayerName = "Test Name";
        var testPageNumber = 3;
        var testPageSize = 9;
        var expectedPageable = Pageable.ofSize(testPageSize).withPage(testPageNumber);
        var expectedContent = List.of(
                PlayerStatsDto.from(UUID.randomUUID(), UUID.randomUUID(), testPlayerName)
        );

        Page<PlayerStatsDto> expectedPage = new PageImpl<>(expectedContent, expectedPageable, 1);

        // given
        given(competitionService.findPlayerStatsByCompetition(
                eq(competitionId),
                argThat(p -> p.getPageSize() == testPageSize && p.getPageNumber() == testPageNumber)
        )).willReturn(expectedPage);

        // when
        mvc.perform(
                        get("/api/competitions/" + competitionId + "/player-stats")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("page", String.valueOf(testPageNumber))
                                .param("size", String.valueOf(testPageSize))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(1)))
                .andExpect(jsonPath("$.content[0].name", is(testPlayerName)));
    }

    @Test
    @DisplayName("GET /api/competitions/:id/league/rounds/:round returns 404 when competition is not found")
    public void getMatchesFromRound_CompetitionNotFound_StatusNotFound() throws Exception {
        var competitionId = UUID.randomUUID();
        var round = 1;

        // given
        given(competitionService.findMatchesByRound(eq(competitionId), eq(round)))
                .willThrow(new ResourceNotFoundException(Competition.class, competitionId));

        // when
        mvc.perform(
                        get("/api/competitions/" + competitionId + "/league/rounds/" + round)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("competition %s could not be found", competitionId)
                )));
    }

    @Test
    @DisplayName("GET /api/competitions/:id/league/rounds/:round returns 404 when competition's phase is not found")
    public void getMatchesFromRound_CompetitionPhaseNotFound_StatusNotFound() throws Exception {
        var competitionId = UUID.randomUUID();
        var round = 1;

        // given
        given(competitionService.findMatchesByRound(eq(competitionId), eq(round)))
                .willThrow(new CompetitionPhaseNotFoundException());

        // when
        mvc.perform(
                        get("/api/competitions/" + competitionId + "/league/rounds/" + round)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        "competition does not have the phase required to execute this action"
                )));
    }

    @Test
    @DisplayName("GET /api/competitions/:id/league/rounds/:round returns 404 when competition's round is not found")
    public void getMatchesFromRound_CompetitionRoundNotFound_StatusNotFound() throws Exception {
        var competitionId = UUID.randomUUID();
        var round = 1;

        // given
        given(competitionService.findMatchesByRound(eq(competitionId), eq(round)))
                .willThrow(new CompetitionRoundNotFoundException(round));

        // when
        mvc.perform(
                        get("/api/competitions/" + competitionId + "/league/rounds/" + round)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("round %d could not be found", round)
                )));
    }

    @Test
    @DisplayName("GET /api/competitions/:id/league/rounds/:round returns 200 when the service returns without exception")
    public void getMatchesFromRound_ServiceReturns_StatusOk() throws Exception {
        var competitionId = UUID.randomUUID();
        var round = 1;

        // given
        given(competitionService.findMatchesByRound(eq(competitionId), eq(round))).willReturn(List.of());

        // when
        mvc.perform(
                        get("/api/competitions/" + competitionId + "/league/rounds/" + round)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }

    private List<UUID> generateMatchIds(int howMany) {
        return IntStream.range(0, howMany).mapToObj(i -> UUID.randomUUID()).toList();
    }

    @Test
    @DisplayName("POST /api/competitions/:id/league/rounds/:round returns 422 when the size of matchIds to assign is invalid")
    public void assignMatchesToRound_MatchIdsSizeInvalid_StatusUnprocessableEntity() throws Exception {
        var invalidSizes = List.of(0, 19, 20, 30, 40);
        var competitionId = UUID.randomUUID();
        var round = 1;

        for (var invalidSize : invalidSizes) {
            var matchesToAssign = generateMatchIds(invalidSize);
            var dto = new UpsertRoundDto(matchesToAssign);
            var json = jsonUpsertRoundDto.write(dto).getJson();

            // when
            mvc.perform(
                            post("/api/competitions/" + competitionId + "/league/rounds/" + round)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.messages",
                            hasEntry("matchIds", List.of("expected between 1 and 18 matches")
                    )));
        }
    }

    @Test
    @DisplayName("POST /api/competitions/:id/league/rounds/:round returns 404 when competition is not found")
    public void assignMatchesToRound_CompetitionNotFound_StatusNotFound() throws Exception {
        var competitionId = UUID.randomUUID();
        var round = 1;
        var matchesToAssign = generateMatchIds(3);
        var dto = new UpsertRoundDto(matchesToAssign);
        var json = jsonUpsertRoundDto.write(dto).getJson();

        // given
        doThrow(new ResourceNotFoundException(Competition.class, competitionId))
                .when(competitionService).assignMatchesToRound(eq(competitionId), eq(round), eq(matchesToAssign));

        // when
        mvc.perform(
                        post("/api/competitions/" + competitionId + "/league/rounds/" + round)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("competition %s could not be found", competitionId)
                )));
    }

    @Test
    @DisplayName("POST /api/competitions/:id/league/rounds/:round returns 404 when competition's phase is not found")
    public void assignMatchesToRound_CompetitionPhaseNotFound_StatusNotFound() throws Exception {
        var competitionId = UUID.randomUUID();
        var round = 1;
        var matchesToAssign = generateMatchIds(3);
        var dto = new UpsertRoundDto(matchesToAssign);
        var json = jsonUpsertRoundDto.write(dto).getJson();

        // given
        doThrow(new CompetitionPhaseNotFoundException())
                .when(competitionService).assignMatchesToRound(eq(competitionId), eq(round), eq(matchesToAssign));

        // when
        mvc.perform(
                        post("/api/competitions/" + competitionId + "/league/rounds/" + round)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        "competition does not have the phase required to execute this action"
                )));
    }

    @Test
    @DisplayName("POST /api/competitions/:id/league/rounds/:round returns 404 when competition's round is not found")
    public void assignMatchesToRound_CompetitionRoundNotFound_StatusNotFound() throws Exception {
        var competitionId = UUID.randomUUID();
        var round = 1;
        var matchesToAssign = generateMatchIds(3);
        var dto = new UpsertRoundDto(matchesToAssign);
        var json = jsonUpsertRoundDto.write(dto).getJson();

        // given
        doThrow(new CompetitionRoundNotFoundException(round))
                .when(competitionService).assignMatchesToRound(eq(competitionId), eq(round), eq(matchesToAssign));

        // when
        mvc.perform(
                        post("/api/competitions/" + competitionId + "/league/rounds/" + round)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("round %d could not be found", round)
                )));
    }

    @Test
    @DisplayName("POST /api/competitions/:id/league/rounds/:round returns 422 when competition's round is not empty")
    public void assignMatchesToRound_CompetitionRoundNotEmpty_StatusUnprocessableEntity() throws Exception {
        var competitionId = UUID.randomUUID();
        var round = 1;
        var matchesToAssign = generateMatchIds(3);
        var dto = new UpsertRoundDto(matchesToAssign);
        var json = jsonUpsertRoundDto.write(dto).getJson();

        // given
        doThrow(new CompetitionRoundNotEmptyException())
                .when(competitionService).assignMatchesToRound(eq(competitionId), eq(round), eq(matchesToAssign));

        // when
        mvc.perform(
                        post("/api/competitions/" + competitionId + "/league/rounds/" + round)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages[0]", is(
                        "only an empty round can have matches assigned to it"
                )));
    }

    @Test
    @DisplayName("POST /api/competitions/:id/league/rounds/:round returns 200 when competition's round is correctly assigned")
    public void assignMatchesToRound_CompetitionRoundAssigned_StatusOk() throws Exception {
        var validSizes = List.of(1, 5, 10, 18);
        var competitionId = UUID.randomUUID();
        var round = 1;

        for (var validSize : validSizes) {
            var matchesToAssign = generateMatchIds(validSize);
            var dto = new UpsertRoundDto(matchesToAssign);
            var json = jsonUpsertRoundDto.write(dto).getJson();

            // when
            mvc.perform(
                            post("/api/competitions/" + competitionId + "/league/rounds/" + round)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());

            verify(competitionService).assignMatchesToRound(eq(competitionId), eq(round), eq(matchesToAssign));
        }
    }

    @Test
    @DisplayName("DELETE /api/competitions/:id/league/rounds/:round returns 200 when competition's round is correctly unassigned")
    public void unassignMatchesFromRound_CompetitionRoundUnassigned_StatusOk() throws Exception {
        var competitionId = UUID.randomUUID();
        var round = 1;

        // when
        mvc.perform(
                        delete("/api/competitions/" + competitionId + "/league/rounds/" + round)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        verify(competitionService).unassignMatchesFromRound(eq(competitionId), eq(round));
    }

    @Test
    @DisplayName("GET /api/competitions/:id/knockout returns 404 when competition is not found")
    public void getKnockoutPhase_CompetitionNotFound_StatusNotFound() throws Exception {
        var competitionId = UUID.randomUUID();

        // given
        given(competitionService.findKnockoutPhase(eq(competitionId)))
                .willThrow(new ResourceNotFoundException(Competition.class, competitionId));

        // when
        mvc.perform(
                        get("/api/competitions/" + competitionId + "/knockout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("competition %s could not be found", competitionId)
                )));
    }

    @Test
    @DisplayName("GET /api/competitions/:id/knockout returns 404 when competition's phase is not found")
    public void getKnockoutPhase_CompetitionPhaseNotFound_StatusNotFound() throws Exception {
        var competitionId = UUID.randomUUID();

        // given
        given(competitionService.findKnockoutPhase(eq(competitionId)))
                .willThrow(new CompetitionPhaseNotFoundException());

        // when
        mvc.perform(
                        get("/api/competitions/" + competitionId + "/knockout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        "competition does not have the phase required to execute this action"
                )));
    }

    @Test
    @DisplayName("GET /api/competitions/:id/knockout returns 200 when the service returns without exception")
    public void getKnockoutPhase_ServiceReturns_StatusOk() throws Exception {
        var competitionId = UUID.randomUUID();

        // given
        given(competitionService.findKnockoutPhase(eq(competitionId))).willReturn(new KnockoutPhaseDto(List.of()));

        // when
        mvc.perform(
                        get("/api/competitions/" + competitionId + "/knockout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/competitions/:id/knockout returns 422 when stages are null")
    public void updateKnockoutPhase_StagesNull_StatusUnprocessableEntity() throws Exception {
        var competitionId = UUID.randomUUID();
        var upsertDto = new UpsertKnockoutTreeDto(null);
        var json = jsonUpsertKnockoutTreeDto.write(upsertDto).getJson();

        // when
        mvc.perform(
                        put("/api/competitions/" + competitionId + "/knockout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages", hasEntry("stages", List.of("field has to be provided"))));
    }

    @Test
    @DisplayName("PUT /api/competitions/:id/knockout returns 422 when the number of stages is incorrect")
    public void updateKnockoutPhase_StagesSizeIncorrect_StatusUnprocessableEntity() throws Exception {
        var competitionId = UUID.randomUUID();
        var incorrectDtos = List.of(
                // 0 stages
                new UpsertKnockoutTreeDto(List.of()),
                // 8 stages
                new UpsertKnockoutTreeDto(
                        IntStream.range(0, 8).mapToObj(i -> new UpsertKnockoutTreeDto.UpsertStage("", List.of())).toList()
                )
        );

        for (var incorrectDto : incorrectDtos) {
            var json = jsonUpsertKnockoutTreeDto.write(incorrectDto).getJson();

            // when
            mvc.perform(
                            put("/api/competitions/" + competitionId + "/knockout")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.messages", hasEntry("stages", List.of("expected between 1 and 7 stages"))));
        }
    }

    @Test
    @DisplayName("PUT /api/competitions/:id/knockout returns 422 when stage name is null")
    public void updateKnockoutPhase_StagesNameNull_StatusUnprocessableEntity() throws Exception {
        var competitionId = UUID.randomUUID();
        var upsertDto = new UpsertKnockoutTreeDto(List.of(new UpsertKnockoutTreeDto.UpsertStage(null, List.of())));
        var json = jsonUpsertKnockoutTreeDto.write(upsertDto).getJson();

        // when
        mvc.perform(
                        put("/api/competitions/" + competitionId + "/knockout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages", hasEntry("stages[0].stage", List.of("field has to be provided"))));
    }

    @Test
    @DisplayName("PUT /api/competitions/:id/knockout returns 422 when stage name is incorrect")
    public void updateKnockoutPhase_StagesNameIncorrect_StatusUnprocessableEntity() throws Exception {
        var competitionId = UUID.randomUUID();
        var incorrectStageNames = List.of(
                "QUARTER__FINAL", "asdf", "semifinal"
        );

        for (var incorrectStageName : incorrectStageNames) {
            var upsertDto = new UpsertKnockoutTreeDto(
                    List.of(new UpsertKnockoutTreeDto.UpsertStage(incorrectStageName, List.of()))
            );
            var json = jsonUpsertKnockoutTreeDto.write(upsertDto).getJson();

            // when
            mvc.perform(
                            put("/api/competitions/" + competitionId + "/knockout")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.messages", hasEntry("stages[0].stage", List.of(
                            "require exactly one of [ROUND_OF_128, ROUND_OF_64, ROUND_OF_32, ROUND_OF_16, QUARTER_FINAL, SEMI_FINAL, FINAL]"
                    ))));
        }
    }

    private List<UpsertKnockoutTreeDto.UpsertKnockoutSlot> generateEmptySlots(int howMany) {
        return IntStream.range(0, howMany)
                .mapToObj(i -> (UpsertKnockoutTreeDto.UpsertKnockoutSlot)new UpsertKnockoutTreeDto.Empty())
                .toList();
    }

    private List<UpsertKnockoutTreeDto.UpsertKnockoutSlot> generateByeSlots(int howMany) {
        return IntStream.range(0, howMany)
                .mapToObj(i -> (UpsertKnockoutTreeDto.UpsertKnockoutSlot)new UpsertKnockoutTreeDto.Bye(UUID.randomUUID()))
                .toList();
    }

    private List<UpsertKnockoutTreeDto.UpsertKnockoutSlot> generateTakenSlots(int howMany) {
        return IntStream.range(0, howMany)
                .mapToObj(i -> (UpsertKnockoutTreeDto.UpsertKnockoutSlot)new UpsertKnockoutTreeDto.Taken(UUID.randomUUID(), UUID.randomUUID()))
                .toList();
    }

    @Test
    @DisplayName("PUT /api/competitions/:id/knockout returns 422 when stage name is duplicated")
    public void updateKnockoutPhase_StagesNameDuplicated_StatusUnprocessableEntity() throws Exception {
        var competitionId = UUID.randomUUID();

        for (var knockoutStageType : KnockoutStage.values()) {
            // double the stage
            var incorrectDto = new UpsertKnockoutTreeDto(
                    IntStream.range(0, 2)
                            .mapToObj(i -> new UpsertKnockoutTreeDto.UpsertStage(
                                    knockoutStageType.name(),
                                    generateEmptySlots(knockoutStageType.getSlots()))
                            )
                            .toList()
            );
            var json = jsonUpsertKnockoutTreeDto.write(incorrectDto).getJson();

            // when
            mvc.perform(
                            put("/api/competitions/" + competitionId + "/knockout")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.messages", hasEntry("general", List.of(
                            "stage names cannot repeat in a single knockout tree"
                    ))));
        }
    }

    @Test
    @DisplayName("PUT /api/competitions/:id/knockout returns 422 when stage slots are null")
    public void updateKnockoutPhase_StagesSlotsNull_StatusUnprocessableEntity() throws Exception {
        var competitionId = UUID.randomUUID();

        for (var knockoutStageType : KnockoutStage.values()) {
            var incorrectDto = new UpsertKnockoutTreeDto(
                    List.of(new UpsertKnockoutTreeDto.UpsertStage(knockoutStageType.name(), null))
            );
            var json = jsonUpsertKnockoutTreeDto.write(incorrectDto).getJson();

            // when
            mvc.perform(
                            put("/api/competitions/" + competitionId + "/knockout")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.messages", hasEntry("stages[0].slots", List.of("field has to be provided"))));
        }
    }

    @Test
    @DisplayName("PUT /api/competitions/:id/knockout returns 422 when stage has incorrect number of slots")
    public void updateKnockoutPhase_StagesSlotsSizeIncorrect_StatusUnprocessableEntity() throws Exception {
        var competitionId = UUID.randomUUID();

        for (var knockoutStageType : KnockoutStage.values()) {
            var incorrectDto = new UpsertKnockoutTreeDto(
                    List.of(new UpsertKnockoutTreeDto.UpsertStage(
                            knockoutStageType.name(),
                            // increment the correct number of slots to make it fail
                            generateEmptySlots(knockoutStageType.getSlots() + 1)
                    ))
            );
            var json = jsonUpsertKnockoutTreeDto.write(incorrectDto).getJson();

            // when
            mvc.perform(
                            put("/api/competitions/" + competitionId + "/knockout")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.messages", hasEntry("stages[0]", List.of(String.format(
                            "stage %s must contain exactly %d slots", knockoutStageType.name(), knockoutStageType.getSlots()
                    )))));
        }
    }

    @Test
    @DisplayName("PUT /api/competitions/:id/knockout returns 422 when slot of type BYE contains null teamId")
    public void updateKnockoutPhase_ByeTeamIdNull_StatusUnprocessableEntity() throws Exception {
        var competitionId = UUID.randomUUID();
        var incorrectDto = new UpsertKnockoutTreeDto(List.of(
                new UpsertKnockoutTreeDto.UpsertStage(
                        KnockoutStage.FINAL.name(), List.of(new UpsertKnockoutTreeDto.Bye(null)))
        ));
        var json = jsonUpsertKnockoutTreeDto.write(incorrectDto).getJson();

        // when
        mvc.perform(
                        put("/api/competitions/" + competitionId + "/knockout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages", hasEntry("stages[0].slots[0].teamId",
                        List.of("field has to be provided")
                )));
    }

    @Test
    @DisplayName("PUT /api/competitions/:id/knockout returns 422 when slot of type TAKEN contains null firstLeg")
    public void updateKnockoutPhase_TakenFirstLegNull_StatusUnprocessableEntity() throws Exception {
        var competitionId = UUID.randomUUID();
        var incorrectDto = new UpsertKnockoutTreeDto(List.of(
                new UpsertKnockoutTreeDto.UpsertStage(
                        KnockoutStage.FINAL.name(), List.of(new UpsertKnockoutTreeDto.Taken(null, null)))
        ));
        var json = jsonUpsertKnockoutTreeDto.write(incorrectDto).getJson();

        // when
        mvc.perform(
                        put("/api/competitions/" + competitionId + "/knockout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages", hasEntry("stages[0].slots[0].firstLeg",
                        List.of("field has to be provided")
                )));
    }

    @Test
    @DisplayName("PUT /api/competitions/:id/knockout returns 422 when multiple TAKEN slots refer to the same match")
    public void updateKnockoutPhase_MultipleTakenSlotsReferToSameMatch_StatusUnprocessableEntity() throws Exception {
        var competitionId = UUID.randomUUID();
        var duplicatedMatchId = UUID.randomUUID();
        var incorrectDtos = List.of(
                // multiple references to the same match in the same slot
                new UpsertKnockoutTreeDto(List.of(new UpsertKnockoutTreeDto.UpsertStage(
                        KnockoutStage.FINAL.name(),
                        List.of(new UpsertKnockoutTreeDto.Taken(duplicatedMatchId, duplicatedMatchId))
                ))),
                // multiple references to the same match in different slots in the same stage
                new UpsertKnockoutTreeDto(List.of(new UpsertKnockoutTreeDto.UpsertStage(
                        KnockoutStage.SEMI_FINAL.name(),
                        List.of(
                                new UpsertKnockoutTreeDto.Taken(duplicatedMatchId, null),
                                new UpsertKnockoutTreeDto.Taken(duplicatedMatchId, null)
                        )
                ))),
                // multiple references to the same match in different stages
                new UpsertKnockoutTreeDto(List.of(
                        new UpsertKnockoutTreeDto.UpsertStage(
                                KnockoutStage.SEMI_FINAL.name(),
                                List.of(
                                        new UpsertKnockoutTreeDto.Taken(duplicatedMatchId, null),
                                        new UpsertKnockoutTreeDto.Empty()
                                )),
                        new UpsertKnockoutTreeDto.UpsertStage(
                                KnockoutStage.FINAL.name(),
                                List.of(
                                        new UpsertKnockoutTreeDto.Taken(duplicatedMatchId, null)
                                ))
                ))
        );

        for (var incorrectDto : incorrectDtos) {
            var json = jsonUpsertKnockoutTreeDto.write(incorrectDto).getJson();

            // when
            mvc.perform(
                            put("/api/competitions/" + competitionId + "/knockout")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.messages", hasEntry("general", List.of(
                            "matches cannot repeat in a single knockout tree"
                    ))));
        }
    }

    private UpsertKnockoutTreeDto createValidEmptyUpsertKnockoutTree() {
        var stages = Arrays.stream(KnockoutStage.values())
                .map(stage -> new UpsertKnockoutTreeDto.UpsertStage(stage.name(), generateEmptySlots(stage.getSlots())))
                .toList();
        return new UpsertKnockoutTreeDto(stages);
    }

    private UpsertKnockoutTreeDto createValidUpsertKnockoutTree() {
        var roundOf16 = KnockoutStage.ROUND_OF_16;
        var roundOf16Stage =
                new UpsertKnockoutTreeDto.UpsertStage(roundOf16.name(), generateTakenSlots(roundOf16.getSlots()));

        var quarterFinal = KnockoutStage.QUARTER_FINAL;
        var quarterFinalStage =
                new UpsertKnockoutTreeDto.UpsertStage(quarterFinal.name(), generateByeSlots(quarterFinal.getSlots()));

        var semiFinal = KnockoutStage.SEMI_FINAL;
        var semiFinalStage =
                new UpsertKnockoutTreeDto.UpsertStage(semiFinal.name(), generateEmptySlots(semiFinal.getSlots()));

        var final_ = KnockoutStage.FINAL;
        var finalStage =
                new UpsertKnockoutTreeDto.UpsertStage(final_.name(), generateTakenSlots(final_.getSlots()));

        return new UpsertKnockoutTreeDto(List.of(roundOf16Stage, quarterFinalStage, semiFinalStage, finalStage));
    }

    @Test
    @DisplayName("PUT /api/competitions/:id/knockout returns 404 when competition is not found")
    public void updateKnockoutPhase_CompetitionNotFound_StatusNotFound() throws Exception {
        var competitionId = UUID.randomUUID();
        var upsertDto = createValidEmptyUpsertKnockoutTree();
        var json = jsonUpsertKnockoutTreeDto.write(upsertDto).getJson();

        // given
        doThrow(new ResourceNotFoundException(Competition.class, competitionId))
                .when(competitionService)
                .updateKnockoutPhase(eq(competitionId), any());

        // when
        mvc.perform(
                        put("/api/competitions/" + competitionId + "/knockout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("competition %s could not be found", competitionId)
                )));
    }

    @Test
    @DisplayName("PUT /api/competitions/:id/knockout returns 404 when competition's phase is not found")
    public void updateKnockoutPhase_CompetitionPhaseNotFound_StatusNotFound() throws Exception {
        var competitionId = UUID.randomUUID();
        var upsertDto = createValidEmptyUpsertKnockoutTree();
        var json = jsonUpsertKnockoutTreeDto.write(upsertDto).getJson();

        // given
        doThrow(new CompetitionPhaseNotFoundException())
                .when(competitionService)
                .updateKnockoutPhase(eq(competitionId), any());

        // when
        mvc.perform(
                        put("/api/competitions/" + competitionId + "/knockout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        "competition does not have the phase required to execute this action"
                )));
    }

    @Test
    @DisplayName("PUT /api/competitions/:id/knockout returns 200 when knockout tree is valid and service does not throw")
    public void updateKnockoutPhase_KnockoutTreeValid_StatusOk() throws Exception {
        var competitionId = UUID.randomUUID();
        var upsertDto = createValidUpsertKnockoutTree();
        var json = jsonUpsertKnockoutTreeDto.write(upsertDto).getJson();

        // when
        mvc.perform(
                        put("/api/competitions/" + competitionId + "/knockout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/competitions/:id/matches returns 400 when criteria not provided")
    public void getLabeledMatchesFromCompetition_FinishedCriteriaNotProvided_StatusBadRequest() throws Exception {
        var competitionId = UUID.randomUUID();

        // when
        mvc.perform(
                        get("/api/competitions/" + competitionId + "/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]", is("query parameter 'finished' not provided")));
    }

    @Test
    @DisplayName("GET /api/competitions/:id/matches returns 200 when fetching matches and pageable is default")
    public void getLabeledMatchesFromCompetition_OnlyFinishedMatchesAndPageableDefault_StatusOk() throws Exception {
        var competitionId = UUID.randomUUID();
        var finished = true;

        // given
        given(competitionService.findLabeledMatches(
                eq(competitionId),
                eq(finished),
                argThat(p -> p.getPageNumber() == 0 && p.getPageSize() == 20)
        )).willReturn(Map.of("1", List.of()));

        // when
        mvc.perform(
                        get("/api/competitions/" + competitionId + "/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("finished", String.valueOf(finished))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasEntry("1", List.of())));
    }

    @Test
    @DisplayName("GET /api/competitions/:id/matches returns 200 when fetching matches and pageable is custom")
    public void getLabeledMatchesFromCompetition_OnlyFinishedMatchesAndPageableCustom_StatusOk() throws Exception {
        var competitionId = UUID.randomUUID();
        var finished = false;
        var page = 1;
        var pageSize = 40;

        // given
        given(competitionService.findLabeledMatches(
                eq(competitionId),
                eq(finished),
                argThat(p -> p.getPageNumber() == page && p.getPageSize() == pageSize)
        )).willReturn(Map.of("FINAL", List.of()));

        // when
        mvc.perform(
                        get("/api/competitions/" + competitionId + "/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("finished", String.valueOf(finished))
                                .param("page", String.valueOf(page))
                                .param("size", String.valueOf(pageSize))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasEntry("FINAL", List.of())));
    }
}
