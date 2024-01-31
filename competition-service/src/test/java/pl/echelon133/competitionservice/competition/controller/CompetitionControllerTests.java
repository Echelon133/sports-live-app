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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.echelon133.competitionservice.competition.model.Competition;
import pl.echelon133.competitionservice.competition.service.CompetitionService;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
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
}
