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
import pl.echelon133.competitionservice.competition.model.Competition;
import pl.echelon133.competitionservice.competition.service.CompetitionService;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
