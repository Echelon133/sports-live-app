package ml.echelon133.matchservice.event.controller;

import ml.echelon133.common.event.dto.MatchEventDetails;
import ml.echelon133.common.event.dto.MatchEventDto;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.match.MatchStatus;
import ml.echelon133.matchservice.MatchServiceApplication;
import ml.echelon133.matchservice.TestValidatorFactory;
import ml.echelon133.matchservice.event.exceptions.MatchEventInvalidException;
import ml.echelon133.matchservice.event.model.dto.InsertMatchEvent;
import ml.echelon133.matchservice.event.service.MatchEventService;
import ml.echelon133.matchservice.match.model.Match;
import ml.echelon133.matchservice.team.constraints.TeamPlayerExists;
import ml.echelon133.matchservice.team.repository.TeamPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.validation.ConstraintValidator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class MatchEventControllerTests {

    private MockMvc mvc;

    // used by @TeamPlayerExists.Validator
    @Mock
    private TeamPlayerRepository teamPlayerRepository;

    @Mock
    private MatchEventService matchEventService;

    @InjectMocks
    private MatchEventExceptionHandler matchEventExceptionHandler;

    @InjectMocks
    private MatchEventController matchEventController;

    private JacksonTester<List<MatchEventDto>> jsonMatchEventDtos;

    private JacksonTester<InsertMatchEvent> jsonInsertMatchEvent;

    private List<InsertMatchEvent> createTestEventsWithMinute(String minute) {
        return List.of(
                new InsertMatchEvent.StatusDto(minute, MatchStatus.FIRST_HALF.name()),
                new InsertMatchEvent.CommentaryDto(minute, "some message"),
                new InsertMatchEvent.CardDto(minute, UUID.randomUUID().toString(), false),
                new InsertMatchEvent.GoalDto(minute, UUID.randomUUID().toString(), null, false),
                new InsertMatchEvent.PenaltyDto(minute, UUID.randomUUID().toString(), true),
                new InsertMatchEvent.SubstitutionDto(minute, UUID.randomUUID().toString(), UUID.randomUUID().toString())
        );
    }

    @BeforeEach
    public void beforeEach() {
        // validators with mocked dependencies which should be used by the standalone MockMvc configuration
        // every time a custom constraint validator is requested
        Map<Class<? extends ConstraintValidator>, ? extends ConstraintValidator> customValidators = Map.of(
                TeamPlayerExists.Validator.class, new TeamPlayerExists.Validator(teamPlayerRepository)
        );
        var validatorFactoryBean = TestValidatorFactory.getInstance(customValidators);

        // use a mapper which understands how to serialize/deserialize MatchEventDto
        var om = MatchServiceApplication.objectMapper();
        JacksonTester.initFields(this, om);

        // the message converter needs to use our object mapper to deserialize test JSON events
        var converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(om);

        mvc = MockMvcBuilders
                .standaloneSetup(matchEventController)
                .setControllerAdvice(matchEventExceptionHandler)
                .setMessageConverters(converter)
                .setValidator(validatorFactoryBean)
                .build();
    }

    @Test
    @DisplayName("GET /api/matches/:id/events returns 200 and a list of events")
    public void getEvents_EventsFound_StatusOk() throws Exception {
        var matchId = UUID.randomUUID();
        var matchEvents = List.of(
                MatchEventDto.from(
                        UUID.randomUUID(),
                        new MatchEventDetails.StatusDto("1", UUID.randomUUID(), MatchStatus.FIRST_HALF, null, null)
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

    @Test
    @DisplayName("POST /api/matches/:id/events returns 404 when resource not found")
    public void processMatchEvent_MatchNotFound_StatusNotFound() throws Exception {
        var matchId = UUID.randomUUID();
        var event = new InsertMatchEvent.StatusDto("1", MatchStatus.FIRST_HALF.name());
        var json = jsonInsertMatchEvent.write(event).getJson();

        // given
        doThrow(new ResourceNotFoundException(Match.class, matchId))
                .when(matchEventService)
                .processEvent(eq(matchId), any());

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("match %s could not be found", matchId)
                )));
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 for all event types when minute is not provided")
    public void processMatchEvent_MinuteNotProvided_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var incorrectEvents = createTestEventsWithMinute(null);

        for (InsertMatchEvent incorrectEvent : incorrectEvents) {
            var json = jsonInsertMatchEvent.write(incorrectEvent).getJson();
            mvc.perform(
                            post("/api/matches/" + matchId + "/events")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("minute", List.of("field has to be provided")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 for all event types when minute format is not correct")
    public void processMatchEvent_MinuteFormatIncorrect_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var incorrectMinutes = List.of(
                "0", "20+3", "asdf", "45-1", "91+3", "119+8"
        );

        for (String incorrectMinute : incorrectMinutes) {
            // create all event types with this incorrect minute
            var incorrectEvents = createTestEventsWithMinute(incorrectMinute);
            for (InsertMatchEvent incorrectEvent : incorrectEvents) {
                var json = jsonInsertMatchEvent.write(incorrectEvent).getJson();
                mvc.perform(
                                post("/api/matches/" + matchId + "/events")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .accept(MediaType.APPLICATION_JSON)
                                        .content(json)
                        )
                        .andExpect(status().isUnprocessableEntity())
                        .andExpect(
                                jsonPath("$.messages", hasEntry("minute", List.of("required minute format: mm(+mm)")))
                        );
            }
        }
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 200 for all event types when minute format is correct")
    public void processMatchEvent_MinuteFormatCorrect_StatusOk() throws Exception {
        var matchId = UUID.randomUUID();
        var correctMinutes = List.of(
                "1", "20", "45", "45+9", "90", "90+3", "120+12"
        );

        // given
        given(teamPlayerRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

        for (String correctMinute: correctMinutes) {
            var correctEvents = createTestEventsWithMinute(correctMinute);
            for (InsertMatchEvent event: correctEvents) {
                var json = jsonInsertMatchEvent.write(event).getJson();
                // when
                mvc.perform(
                                post("/api/matches/" + matchId + "/events")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .accept(MediaType.APPLICATION_JSON)
                                        .content(json)
                        )
                        .andExpect(status().isOk());
            }
        }
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when STATUS event's target status is not provided")
    public void processMatchEvent_TargetStatusNotProvided_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var event = new InsertMatchEvent.StatusDto("1", null);
        var json = jsonInsertMatchEvent.write(event).getJson();

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("targetStatus", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when STATUS event's target status is not correct")
    public void processMatchEvent_TargetStatusIncorrect_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var incorrectStatuses = List.of(
                "ADFSDF", "asdfasdf", "NONSTARTED", "FIRSTHALF", "FINISH"
        );

        for (String incorrectStatus: incorrectStatuses) {
            var event = new InsertMatchEvent.StatusDto("1", incorrectStatus);
            var json = jsonInsertMatchEvent.write(event).getJson();
            // when
            mvc.perform(
                            post("/api/matches/" + matchId + "/events")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("targetStatus", List.of("provided status is invalid")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 200 when STATUS event's target status is correct")
    public void processMatchEvent_TargetStatusCorrect_StatusOk() throws Exception {
        var matchId = UUID.randomUUID();
        var correctStatuses = List.of(
            // test upper case
            "NOT_STARTED", "FIRST_HALF", "HALF_TIME",
            "SECOND_HALF", "FINISHED", "EXTRA_TIME",
            "PENALTIES", "POSTPONED", "ABANDONED",
            // test lower case
            "not_started", "first_half", "half_time",
            "second_half", "finished", "extra_time",
            "penalties", "postponed", "abandoned",
            // test mixed case
            "NOT_started", "FIRST_half", "HALF_time",
            "SECOND_half", "FINIshed", "EXTRA_time",
            "PENALties", "POSTponed", "ABANdoned"
        );

        for (String correctStatus: correctStatuses) {
            var event = new InsertMatchEvent.StatusDto("1", correctStatus);
            var json = jsonInsertMatchEvent.write(event).getJson();
            // when
            mvc.perform(
                            post("/api/matches/" + matchId + "/events")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when COMMENTARY event's message is not provided")
    public void processMatchEvent_MessageNotProvided_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var event = new InsertMatchEvent.CommentaryDto("1", null);
        var json = jsonInsertMatchEvent.write(event).getJson();

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("message", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when COMMENTARY event's message is not correct")
    public void processMatchEvent_MessageIncorrect_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var incorrectMessages = List.of(
                // empty
                "",
                // 1001 characters
                "aGiERX2xYjvgpmHZJzMExG2QmLwNS96cBKPqkZC9YLeSP2g5urh2QHaCdc8XkyxRbPLZkxgBUW8rYucuV60N4MntUe0zdB3iUma1WFYtE0tZhykw8UquyWe42DArQnL0CF8Ykwwvk3ZgVBTTDwKLAk34U1xBfWSTbku8S3Pa1knSN4ymTxpkRAy5rhTuRwSauLwfpdKrNA9YfaHkMLjrY9qmSyzKfRR5k7FYhrcBr0FjFfVU5bC19FbEhFEBhdBFa6UbQUV1iJfyuKSVVVkbU1R6K3Vy577zCexaBqB58b5YE8vzr5pDjCgyhqbdXugYqWMhRzi4Q2GJtzwReLLfBRd7aa9xvQ4e4XYx1iWeJwRd0YE6t4B5aybNT1LpYwPGyaDpK52y6ckN5mCeZgFY6NPKA1WwkWhMJAAfpmib24VRi4uT67V7MMnJn4zxyGZq4X4B0GbUqMk9yut81628umjY8GNa2HLrD6PnmutDKH946Qr5mtDY04r7DzuwzDAcxzVy7ry6B6UpVcwE7S1bujdVe1LScuGcSLgU0tug1cGd6RuYeQ8gCccg8dVpMK6u7Xj9VX56nfCzbpCu9EYQQMipHRqY7bRGQ6zzmCNAfuj6w2uuXpiE5j0kHSKxr1kimxXJbU0nigTA31pW75Vbu0d9zvb6ZHmL5qHHx18S0cMteUqZA4xwVHgtxvZ3VJpBa2amjLWewCPqS4TLuHq60J3rCeFC7HFau6XdR0Wu87X9efbHq1qU2ifiQw1NFDSkGSZX3AkZxcJQcUuJDaQgSnDgveCxLHigPrpJkgmL2zF2UBxYAUjiXnWJbYGtLfQ30nVaCGr87QjKb3TUWzjd4rfZGTp9eTL0TiE8vrGFhZa7a5T1AKfy6LGNGRrtwh37XDHmnAeBiiXBCrRhu1wYXDvYuMyfkb4m7Xq9izAGbVjj3cyLMUABKnVWTLbnKAaziYFneNr4DYKaH0DHavSZBtC8Num6J4QawMwKSkNz2"
        );

        for (String incorrectMessage: incorrectMessages) {
            var event = new InsertMatchEvent.CommentaryDto("1", incorrectMessage);
            var json = jsonInsertMatchEvent.write(event).getJson();
            // when
            mvc.perform(
                            post("/api/matches/" + matchId + "/events")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("message", List.of("should contain between 1 and 1000 characters")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 200 when COMMENTARY event's message is correct")
    public void processMatchEvent_MessageCorrect_StatusOk() throws Exception {
        var matchId = UUID.randomUUID();
        var correctMessages = List.of(
                // 1 character
                "a",
                // 1000 characters
                "GiERX2xYjvgpmHZJzMExG2QmLwNS96cBKPqkZC9YLeSP2g5urh2QHaCdc8XkyxRbPLZkxgBUW8rYucuV60N4MntUe0zdB3iUma1WFYtE0tZhykw8UquyWe42DArQnL0CF8Ykwwvk3ZgVBTTDwKLAk34U1xBfWSTbku8S3Pa1knSN4ymTxpkRAy5rhTuRwSauLwfpdKrNA9YfaHkMLjrY9qmSyzKfRR5k7FYhrcBr0FjFfVU5bC19FbEhFEBhdBFa6UbQUV1iJfyuKSVVVkbU1R6K3Vy577zCexaBqB58b5YE8vzr5pDjCgyhqbdXugYqWMhRzi4Q2GJtzwReLLfBRd7aa9xvQ4e4XYx1iWeJwRd0YE6t4B5aybNT1LpYwPGyaDpK52y6ckN5mCeZgFY6NPKA1WwkWhMJAAfpmib24VRi4uT67V7MMnJn4zxyGZq4X4B0GbUqMk9yut81628umjY8GNa2HLrD6PnmutDKH946Qr5mtDY04r7DzuwzDAcxzVy7ry6B6UpVcwE7S1bujdVe1LScuGcSLgU0tug1cGd6RuYeQ8gCccg8dVpMK6u7Xj9VX56nfCzbpCu9EYQQMipHRqY7bRGQ6zzmCNAfuj6w2uuXpiE5j0kHSKxr1kimxXJbU0nigTA31pW75Vbu0d9zvb6ZHmL5qHHx18S0cMteUqZA4xwVHgtxvZ3VJpBa2amjLWewCPqS4TLuHq60J3rCeFC7HFau6XdR0Wu87X9efbHq1qU2ifiQw1NFDSkGSZX3AkZxcJQcUuJDaQgSnDgveCxLHigPrpJkgmL2zF2UBxYAUjiXnWJbYGtLfQ30nVaCGr87QjKb3TUWzjd4rfZGTp9eTL0TiE8vrGFhZa7a5T1AKfy6LGNGRrtwh37XDHmnAeBiiXBCrRhu1wYXDvYuMyfkb4m7Xq9izAGbVjj3cyLMUABKnVWTLbnKAaziYFneNr4DYKaH0DHavSZBtC8Num6J4QawMwKSkNz2"
        );

        for (String correctMessage: correctMessages) {
            var event = new InsertMatchEvent.CommentaryDto("1", correctMessage);
            var json = jsonInsertMatchEvent.write(event).getJson();
            // when
            mvc.perform(
                            post("/api/matches/" + matchId + "/events")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when CARD event's cardedPlayerId is not provided")
    public void processMatchEvent_CardedPlayerIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var event = new InsertMatchEvent.CardDto("1", null, false);
        var json = jsonInsertMatchEvent.write(event).getJson();

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("cardedPlayerId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when CARD event's cardedPlayerId is not a uuid")
    public void processMatchEvent_CardedPlayerIdNotUUID_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var event = new InsertMatchEvent.CardDto("1", "some-id", false);
        var json = jsonInsertMatchEvent.write(event).getJson();

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("cardedPlayerId", List.of("id does not belong to a valid team player")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when CARD event's cardedPlayerId does not exist")
    public void processMatchEvent_CardedPlayerIdNotFound_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var cardedPlayerId = UUID.randomUUID();
        var event = new InsertMatchEvent.CardDto("1", cardedPlayerId.toString(), false);
        var json = jsonInsertMatchEvent.write(event).getJson();

        // given
        given(teamPlayerRepository.existsByIdAndDeletedFalse(eq(cardedPlayerId))).willReturn(false);

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("cardedPlayerId", List.of("id does not belong to a valid team player")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 200 when CARD event's cardedPlayerId exists")
    public void processMatchEvent_CardedPlayerIdFound_StatusOk() throws Exception {
        var matchId = UUID.randomUUID();
        var cardedPlayerId = UUID.randomUUID();
        var event = new InsertMatchEvent.CardDto("1", cardedPlayerId.toString(), false);
        var json = jsonInsertMatchEvent.write(event).getJson();

        // given
        given(teamPlayerRepository.existsByIdAndDeletedFalse(eq(cardedPlayerId))).willReturn(true);

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when GOAL event's scoringPlayerId is not provided")
    public void processMatchEvent_ScoringPlayerIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var event = new InsertMatchEvent.GoalDto("1", null, UUID.randomUUID().toString(), false);
        var json = jsonInsertMatchEvent.write(event).getJson();

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("scoringPlayerId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when GOAL event's scoringPlayerId is not a uuid")
    public void processMatchEvent_ScoringPlayerIdNotUUID_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var event = new InsertMatchEvent.GoalDto("1", "some-id", UUID.randomUUID().toString(), false);
        var json = jsonInsertMatchEvent.write(event).getJson();

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("scoringPlayerId", List.of("id does not belong to a valid team player")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when GOAL event's scoringPlayerId does not exist")
    public void processMatchEvent_ScoringPlayerIdNotFound_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var scoringPlayerId = UUID.randomUUID();
        var event = new InsertMatchEvent.GoalDto(
                "1", scoringPlayerId.toString(), UUID.randomUUID().toString(), false
        );
        var json = jsonInsertMatchEvent.write(event).getJson();

        // given
        given(teamPlayerRepository.existsByIdAndDeletedFalse(any())).willAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return !id.equals(scoringPlayerId);
        });

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("scoringPlayerId", List.of("id does not belong to a valid team player")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 200 when GOAL event's scoringPlayerId exists")
    public void processMatchEvent_ScoringPlayerIdFound_StatusOk() throws Exception {
        var matchId = UUID.randomUUID();
        var scoringPlayerId = UUID.randomUUID();
        var event = new InsertMatchEvent.GoalDto(
                "1", scoringPlayerId.toString(), UUID.randomUUID().toString(), false
        );
        var json = jsonInsertMatchEvent.write(event).getJson();

        // given
        given(teamPlayerRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 200 when GOAL event's assistingPlayerId is not provided because it's optional")
    public void processMatchEvent_AssistingPlayerIdNotProvided_StatusOk() throws Exception {
        var matchId = UUID.randomUUID();
        var scoringPlayerId = UUID.randomUUID();
        var event = new InsertMatchEvent.GoalDto("1", scoringPlayerId.toString(), null, false);
        var json = jsonInsertMatchEvent.write(event).getJson();

        // given
        given(teamPlayerRepository.existsByIdAndDeletedFalse(scoringPlayerId)).willReturn(true);

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when GOAL event's assistingPlayerId is not a uuid")
    public void processMatchEvent_AssistingPlayerIdNotUUID_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var scoringPlayerId = UUID.randomUUID();
        var event = new InsertMatchEvent.GoalDto("1", scoringPlayerId.toString(), "asdf", false);
        var json = jsonInsertMatchEvent.write(event).getJson();

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("assistingPlayerId", List.of("id does not belong to a valid team player")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when GOAL event's assistingPlayerId does not exist")
    public void processMatchEvent_AssistingPlayerIdNotFound_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var scoringPlayerId = UUID.randomUUID();
        var assistingPlayerId = UUID.randomUUID();
        var event = new InsertMatchEvent.GoalDto(
                "1", scoringPlayerId.toString(), assistingPlayerId.toString(), false
        );
        var json = jsonInsertMatchEvent.write(event).getJson();

        // given
        given(teamPlayerRepository.existsByIdAndDeletedFalse(any())).willAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return !id.equals(assistingPlayerId);
        });

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("assistingPlayerId", List.of("id does not belong to a valid team player")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when GOAL event's scoringPlayerId and assistingPlayerId are the same")
    public void processMatchEvent_ScoringAndAssistingPlayerIdentical_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var event = new InsertMatchEvent.GoalDto(
                "1", playerId.toString(), playerId.toString(), false
        );
        var json = jsonInsertMatchEvent.write(event).getJson();

        // given
        given(teamPlayerRepository.existsByIdAndDeletedFalse(playerId)).willReturn(true);

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("general", List.of("the id of scoring and assisting player must not be identical")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 200 when GOAL event's assistingPlayerId exists")
    public void processMatchEvent_AssistingPlayerIdFound_StatusOk() throws Exception {
        var matchId = UUID.randomUUID();
        var event = new InsertMatchEvent.GoalDto(
                "1", UUID.randomUUID().toString(), UUID.randomUUID().toString(), false
        );
        var json = jsonInsertMatchEvent.write(event).getJson();

        // given
        given(teamPlayerRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when PENALTY event's shootingPlayerId is not provided")
    public void processMatchEvent_ShootingPlayerIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var event = new InsertMatchEvent.PenaltyDto("1", null, false);
        var json = jsonInsertMatchEvent.write(event).getJson();

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("shootingPlayerId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when PENALTY event's shootingPlayerId is not a uuid")
    public void processMatchEvent_ShootingPlayerIdNotUUID_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var event = new InsertMatchEvent.PenaltyDto("1", "some-id", false);
        var json = jsonInsertMatchEvent.write(event).getJson();

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("shootingPlayerId", List.of("id does not belong to a valid team player")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when PENALTY event's shootingPlayerId does not exist")
    public void processMatchEvent_ShootingPlayerIdNotFound_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var shootingPlayerId = UUID.randomUUID();
        var event = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), false);
        var json = jsonInsertMatchEvent.write(event).getJson();

        // given
        given(teamPlayerRepository.existsByIdAndDeletedFalse(shootingPlayerId)).willReturn(false);

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("shootingPlayerId", List.of("id does not belong to a valid team player")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 200 when PENALTY event's shootingPlayerId exists")
    public void processMatchEvent_ShootingPlayerIdFound_StatusOk() throws Exception {
        var matchId = UUID.randomUUID();
        var shootingPlayerId = UUID.randomUUID();
        var event = new InsertMatchEvent.PenaltyDto("1", shootingPlayerId.toString(), false);
        var json = jsonInsertMatchEvent.write(event).getJson();

        // given
        given(teamPlayerRepository.existsByIdAndDeletedFalse(shootingPlayerId)).willReturn(true);

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when SUBSTITUTION event's playerInId is not provided")
    public void processMatchEvent_PlayerInIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var event = new InsertMatchEvent.SubstitutionDto("1", null, UUID.randomUUID().toString());
        var json = jsonInsertMatchEvent.write(event).getJson();

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("playerInId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when SUBSTITUTION event's playerInId is not a uuid")
    public void processMatchEvent_PlayerInIdNotUUID_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var event = new InsertMatchEvent.SubstitutionDto("1", "some-id", UUID.randomUUID().toString());
        var json = jsonInsertMatchEvent.write(event).getJson();

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("playerInId", List.of("id does not belong to a valid team player")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when SUBSTITUTION event's playerInId does not exist")
    public void processMatchEvent_PlayerInIdNotFound_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var playerInId = UUID.randomUUID();
        var event = new InsertMatchEvent.SubstitutionDto("1", playerInId.toString(), UUID.randomUUID().toString());
        var json = jsonInsertMatchEvent.write(event).getJson();

        // given
        given(teamPlayerRepository.existsByIdAndDeletedFalse(any())).willAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return !id.equals(playerInId);
        });

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("playerInId", List.of("id does not belong to a valid team player")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 200 when SUBSTITUTION event's playerInId exists")
    public void processMatchEvent_PlayerInIdFound_StatusOk() throws Exception {
        var matchId = UUID.randomUUID();
        var event = new InsertMatchEvent.SubstitutionDto("1", UUID.randomUUID().toString(), UUID.randomUUID().toString());
        var json = jsonInsertMatchEvent.write(event).getJson();

        // given
        given(teamPlayerRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when SUBSTITUTION event's playerOutId is not provided")
    public void processMatchEvent_PlayerOutIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var event = new InsertMatchEvent.SubstitutionDto("1", UUID.randomUUID().toString(), null);
        var json = jsonInsertMatchEvent.write(event).getJson();

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("playerOutId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when SUBSTITUTION event's playerOutId is not a uuid")
    public void processMatchEvent_PlayerOutIdNotUUID_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var event = new InsertMatchEvent.SubstitutionDto("1", UUID.randomUUID().toString(), "some-id");
        var json = jsonInsertMatchEvent.write(event).getJson();

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("playerOutId", List.of("id does not belong to a valid team player")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when SUBSTITUTION event's playerOutId does not exist")
    public void processMatchEvent_PlayerOutIdNotFound_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var playerOutId = UUID.randomUUID();
        var event = new InsertMatchEvent.SubstitutionDto("1", UUID.randomUUID().toString(), playerOutId.toString());
        var json = jsonInsertMatchEvent.write(event).getJson();

        // given
        given(teamPlayerRepository.existsByIdAndDeletedFalse(any())).willAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return !id.equals(playerOutId);
        });

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("playerOutId", List.of("id does not belong to a valid team player")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 200 when SUBSTITUTION event's playerOutId exists")
    public void processMatchEvent_PlayerOutIdFound_StatusOk() throws Exception {
        var matchId = UUID.randomUUID();
        var event = new InsertMatchEvent.SubstitutionDto("1", UUID.randomUUID().toString(), UUID.randomUUID().toString());
        var json = jsonInsertMatchEvent.write(event).getJson();

        // given
        given(teamPlayerRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when SUBSTITUTION event's playerInId and playerOutId are the same")
    public void processMatchEvent_InAndOutPlayerIdentical_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var event = new InsertMatchEvent.SubstitutionDto("1", playerId.toString(), playerId.toString());
        var json = jsonInsertMatchEvent.write(event).getJson();

        // given
        given(teamPlayerRepository.existsByIdAndDeletedFalse(playerId)).willReturn(true);

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("general", List.of("the id of in and out players must not be identical")))
                );
    }

    @Test
    @DisplayName("POST /api/matches/:id/events returns 422 when processing throws on invalid event")
    public void processMatchEvent_InvalidEvent_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var event = new InsertMatchEvent.StatusDto("1", MatchStatus.FIRST_HALF.name());
        var json = jsonInsertMatchEvent.write(event).getJson();

        // given
        doThrow(new MatchEventInvalidException("exception message"))
                .when(matchEventService)
                .processEvent(eq(matchId), any());

        // when
        mvc.perform(
                        post("/api/matches/" + matchId + "/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages[0]", is("exception message")));
    }
}
