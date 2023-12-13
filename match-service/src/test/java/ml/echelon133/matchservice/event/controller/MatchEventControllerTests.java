package ml.echelon133.matchservice.event.controller;

import ml.echelon133.common.event.dto.MatchEventDetails;
import ml.echelon133.common.event.dto.MatchEventDto;
import ml.echelon133.common.match.MatchStatus;
import ml.echelon133.matchservice.MatchServiceApplication;
import ml.echelon133.matchservice.event.service.MatchEventService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class MatchEventControllerTests {

    private MockMvc mvc;

    @Mock
    private MatchEventService matchEventService;

    @InjectMocks
    private MatchEventExceptionHandler matchEventExceptionHandler;

    @InjectMocks
    private MatchEventController matchEventController;

    private JacksonTester<List<MatchEventDto>> jsonMatchEventDtos;

    @BeforeEach
    public void beforeEach() {
        // use a mapper which understands how to serialize/deserialize MatchEventDto
        var om = MatchServiceApplication.objectMapper();
        JacksonTester.initFields(this, om);

        mvc = MockMvcBuilders
                .standaloneSetup(matchEventController)
                .setControllerAdvice(matchEventExceptionHandler)
                .build();
    }

    @Test
    @DisplayName("GET /api/matches/:id/events returns 200 and a list of events")
    public void getEvents_EventsFound_StatusOk() throws Exception {
        var matchId = UUID.randomUUID();
        var matchEvents = List.of(
                MatchEventDto.from(
                        UUID.randomUUID(),
                        new MatchEventDetails.StatusDto("1", UUID.randomUUID(), MatchStatus.FIRST_HALF)
                )
        );
        var expectedJson = jsonMatchEventDtos.write(matchEvents).getJson();

        // given
        given(matchEventService.findAllByMatchId(matchId)).willReturn(matchEvents);

        // when
        mvc.perform(
                    get("/api/matches/" + matchId + "/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }
}
