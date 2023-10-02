package ml.echelon133.matchservice.match.controller;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.match.MatchStatus;
import ml.echelon133.common.match.dto.MatchDto;
import ml.echelon133.common.match.dto.MatchStatusDto;
import ml.echelon133.matchservice.MatchServiceApplication;
import ml.echelon133.matchservice.match.TestMatchDto;
import ml.echelon133.matchservice.match.TestUpsertMatchDto;
import ml.echelon133.matchservice.match.model.Match;
import ml.echelon133.matchservice.match.model.UpsertMatchDto;
import ml.echelon133.matchservice.match.service.MatchService;
import ml.echelon133.matchservice.referee.model.Referee;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.venue.model.Venue;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class MatchControllerTests {

    private MockMvc mvc;

    @Mock
    private MatchService matchService;

    @InjectMocks
    private MatchExceptionHandler matchExceptionHandler;

    @InjectMocks
    private MatchController matchController;

    private JacksonTester<MatchDto> jsonMatchDto;

    private JacksonTester<MatchStatusDto> jsonMatchStatusDto;

    private JacksonTester<UpsertMatchDto> jsonUpsertMatchDto;

    @BeforeEach
    public void beforeEach() {
        // use a mapper with date/time modules, otherwise LocalDate won't work
        var om = MatchServiceApplication.objectMapper();

        JacksonTester.initFields(this, om);
        mvc = MockMvcBuilders
                .standaloneSetup(matchController)
                .setControllerAdvice(matchExceptionHandler)
                .build();
    }

    @Test
    @DisplayName("GET /api/matches/:id returns 404 when resource not found")
    public void getMatch_MatchNotFound_StatusNotFound() throws Exception {
        var matchId = UUID.randomUUID();

        // given
        given(matchService.findById(matchId)).willThrow(
                new ResourceNotFoundException(Match.class, matchId)
        );

        // when
        mvc.perform(
                        get("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("match %s could not be found", matchId)
                )));
    }

    @Test
    @DisplayName("GET /api/matches/:id returns 200 and a valid entity if entity found")
    public void getMatch_MatchFound_StatusOk() throws Exception {
        var matchId = UUID.randomUUID();

        var matchDto = TestMatchDto.builder().id(matchId).build();
        var expectedJson = jsonMatchDto.write(matchDto).getJson();

        // given
        given(matchService.findById(matchId)).willReturn(matchDto);

        // when
        mvc.perform(
                        get("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("GET /api/matches/:id/status returns 404 when resource not found")
    public void getMatchStatus_MatchNotFound_StatusNotFound() throws Exception {
        var matchId = UUID.randomUUID();

        // given
        given(matchService.findStatusById(matchId)).willThrow(
                new ResourceNotFoundException(Match.class, matchId)
        );

        // when
        mvc.perform(
                        get("/api/matches/" + matchId + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("match %s could not be found", matchId)
                )));
    }

    @Test
    @DisplayName("GET /api/matches/:id/status returns 200 and a valid entity if entity found")
    public void getMatchStatus_MatchFound_StatusOk() throws Exception {
        var matchId = UUID.randomUUID();

        var matchStatusDto = MatchStatusDto.from(MatchStatus.NOT_STARTED.toString());
        var expectedJson = jsonMatchStatusDto.write(matchStatusDto).getJson();

        // given
        given(matchService.findStatusById(matchId)).willReturn(matchStatusDto);

        // when
        mvc.perform(
                        get("/api/matches/" + matchId + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("POST /api/matches returns 422 when homeTeamId is not provided")
    public void createMatch_HomeTeamIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertMatchDto.builder().homeTeamId(null).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("homeTeamId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/matches returns 422 when homeTeamId is not an uuid")
    public void createMatch_HomeTeamIdInvalidUuid_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertMatchDto.builder().homeTeamId("a").build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("homeTeamId", List.of("not a valid uuid")))
                );
    }

    @Test
    @DisplayName("POST /api/matches returns 422 when awayTeamId is not provided")
    public void createMatch_AwayTeamIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertMatchDto.builder().awayTeamId(null).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("awayTeamId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/matches returns 422 when awayTeamId is not an uuid")
    public void createMatch_AwayTeamIdInvalidUuid_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertMatchDto.builder().awayTeamId("a").build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("awayTeamId", List.of("not a valid uuid")))
                );
    }

    @Test
    @DisplayName("POST /api/matches returns 422 when venueId is not provided")
    public void createMatch_VenueIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertMatchDto.builder().venueId(null).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("venueId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/matches returns 422 when venueId is not an uuid")
    public void createMatch_VenueIdInvalidUuid_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertMatchDto.builder().venueId("a").build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("venueId", List.of("not a valid uuid")))
                );
    }

    @Test
    @DisplayName("POST /api/matches returns 422 when competitionId is not provided")
    public void createMatch_CompetitionIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertMatchDto.builder().competitionId(null).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("competitionId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/matches returns 422 when competitionId is not an uuid")
    public void createMatch_CompetitionIdInvalidUuid_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertMatchDto.builder().competitionId("a").build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("competitionId", List.of("not a valid uuid")))
                );
    }

    @Test
    @DisplayName("POST /api/matches returns 422 when startTimeUTC is not provided")
    public void createMatch_StartTimeUTCNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertMatchDto.builder().startTimeUTC(null).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("startTimeUTC", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/matches returns 422 when startTimeUTC is not a date in a valid format")
    public void createMatch_StartTimeUTCInvalidFormat_StatusUnprocessableEntity() throws Exception {
        var invalidDates = List.of(
                "asdf", // not a date
                "2023/01/01", // invalid because hours are not specified
                "2023/13/01 19:00", // invalid because the month does not exist
                "2023-01-01 19:00"  // invalid because of wrong separator
        );

        for (String invalidDate : invalidDates) {
            var contentDto = TestUpsertMatchDto.builder().startTimeUTC(invalidDate).build();
            var json = jsonUpsertMatchDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/matches")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("startTimeUTC", List.of("required date format yyyy/MM/d H:m")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/matches returns 200 when refereeId is not provided (it's optional)")
    public void createMatch_RefereeIdNotProvided_StatusOk() throws Exception {
        var contentDto = TestUpsertMatchDto.builder().refereeId(null).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/matches returns 422 when refereeId is not an uuid")
    public void createMatch_RefereeIdInvalidUuid_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertMatchDto.builder().refereeId("a").build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("refereeId", List.of("not a valid uuid")))
                );
    }

    @Test
    @DisplayName("POST /api/matches returns 422 when home team not found")
    public void createMatch_HomeTeamNotFound_StatusUnprocessableEntity() throws Exception {
        var homeTeamId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder()
                .homeTeamId(homeTeamId.toString()).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        // given
        given(matchService.createMatch(argThat(
                m -> m.getHomeTeamId().equals(homeTeamId.toString())
        ))).willThrow(new ResourceNotFoundException(Team.class, homeTeamId));

        // when
        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath(
                                "$.messages",
                                hasEntry("homeTeamId", List.of(String.format("team %s could not be found", homeTeamId)))
                        )
                );

    }

    @Test
    @DisplayName("POST /api/matches returns 422 when away team not found")
    public void createMatch_AwayTeamNotFound_StatusUnprocessableEntity() throws Exception {
        var awayTeamId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder()
                .awayTeamId(awayTeamId.toString()).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        // given
        given(matchService.createMatch(argThat(
                m -> m.getAwayTeamId().equals(awayTeamId.toString())
        ))).willThrow(new ResourceNotFoundException(Team.class, awayTeamId));

        // when
        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath(
                                "$.messages",
                                hasEntry("awayTeamId", List.of(String.format("team %s could not be found", awayTeamId)))
                        )
                );
    }

    @Test
    @DisplayName("POST /api/matches returns 422 when venue not found")
    public void createMatch_VenueNotFound_StatusUnprocessableEntity() throws Exception {
        var venueId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder()
                .venueId(venueId.toString()).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        // given
        given(matchService.createMatch(argThat(
                m -> m.getVenueId().equals(venueId.toString())
        ))).willThrow(new ResourceNotFoundException(Venue.class, venueId));

        // when
        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath(
                                "$.messages",
                                hasEntry("venueId", List.of(String.format("venue %s could not be found", venueId)))
                        )
                );
    }

    @Test
    @DisplayName("POST /api/matches returns 422 when referee not found")
    public void createMatch_RefereeNotFound_StatusUnprocessableEntity() throws Exception {
        var refereeId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder()
                .refereeId(refereeId.toString()).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        // given
        given(matchService.createMatch(argThat(
                m -> m.getRefereeId().equals(refereeId.toString())
        ))).willThrow(new ResourceNotFoundException(Referee.class, refereeId));

        // when
        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath(
                                "$.messages",
                                hasEntry("refereeId", List.of(String.format("referee %s could not be found", refereeId)))
                        )
                );
    }

    @Test
    @DisplayName("POST /api/matches returns 200 and calls the service when request body valid")
    public void createMatch_ValuesInBodyCorrect_StatusOkAndCallsService() throws Exception {
        var expectedDto = TestMatchDto.builder()
                .startTimeUTC(LocalDateTime.of(2023, 1, 1, 19, 0))
                .build();
        var expectedJson = jsonMatchDto.write(expectedDto).getJson();

        var contentDto = TestUpsertMatchDto.builder()
                .homeTeamId(expectedDto.getHomeTeam().getId().toString())
                .startTimeUTC("2023/01/01 19:00")
                .awayTeamId(expectedDto.getAwayTeam().getId().toString())
                .venueId(expectedDto.getVenue().getId().toString())
                .refereeId(expectedDto.getReferee().getId().toString())
                .competitionId(expectedDto.getCompetitionId().toString())
                .build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();


        // given
        given(matchService.createMatch(argThat(m ->
                m.getHomeTeamId().equals(contentDto.getHomeTeamId()) &&
                m.getAwayTeamId().equals(contentDto.getAwayTeamId()) &&
                m.getVenueId().equals(contentDto.getVenueId()) &&
                m.getRefereeId().equals(contentDto.getRefereeId()) &&
                m.getCompetitionId().equals(contentDto.getCompetitionId()) &&
                m.getStartTimeUTC().equals(contentDto.getStartTimeUTC())
        ))).willReturn(expectedDto);

        // when
        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("PUT /api/match/:id returns 422 when homeTeamId is not provided")
    public void updateMatch_HomeTeamIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder().homeTeamId(null).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("homeTeamId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 422 when homeTeamId is not an uuid")
    public void updateMatch_HomeTeamIdInvalidUuid_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder().homeTeamId("a").build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("homeTeamId", List.of("not a valid uuid")))
                );
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 422 when awayTeamId is not provided")
    public void updateMatch_AwayTeamIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder().awayTeamId(null).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("awayTeamId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 422 when awayTeamId is not an uuid")
    public void updateMatch_AwayTeamIdInvalidUuid_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder().awayTeamId("a").build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("awayTeamId", List.of("not a valid uuid")))
                );
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 422 when venueId is not provided")
    public void updateMatch_VenueIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder().venueId(null).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("venueId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 422 when venueId is not an uuid")
    public void updateMatch_VenueIdInvalidUuid_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder().venueId("a").build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("venueId", List.of("not a valid uuid")))
                );
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 422 when competitionId is not provided")
    public void updateMatch_CompetitionIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder().competitionId(null).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("competitionId", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 422 when competitionId is not an uuid")
    public void updateMatch_CompetitionIdInvalidUuid_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder().competitionId("a").build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("competitionId", List.of("not a valid uuid")))
                );
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 422 when startTimeUTC is not provided")
    public void updateMatch_StartTimeUTCNotProvided_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder().startTimeUTC(null).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("startTimeUTC", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 422 when startTimeUTC is not a date in a valid format")
    public void updateMatch_StartTimeUTCInvalidFormat_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();

        var invalidDates = List.of(
                "asdf", // not a date
                "2023/01/01", // invalid because hours are not specified
                "2023/13/01 19:00", // invalid because the month does not exist
                "2023-01-01 19:00"  // invalid because of wrong separator
        );

        for (String invalidDate : invalidDates) {
            var contentDto = TestUpsertMatchDto.builder().startTimeUTC(invalidDate).build();
            var json = jsonUpsertMatchDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/matches/" + matchId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("startTimeUTC", List.of("required date format yyyy/MM/d H:m")))
                    );
        }
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 200 when refereeId is not provided (it's optional)")
    public void updateMatch_RefereeIdNotProvided_StatusOk() throws Exception {
        var matchId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder().refereeId(null).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 422 when refereeId is not an uuid")
    public void updateMatch_RefereeIdInvalidUuid_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder().refereeId("a").build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("refereeId", List.of("not a valid uuid")))
                );
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 404 when match not found")
    public void updateMatch_MatchNotFound_StatusNotFound() throws Exception {
        var matchId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder().build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        // given
        given(matchService.updateMatch(eq(matchId), any())).willThrow(
                new ResourceNotFoundException(Match.class, matchId)
        );

        // when
        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages", hasItem(String.format("match %s could not be found", matchId))));
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 422 when home team not found")
    public void updateMatch_HomeTeamNotFound_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var homeTeamId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder()
                .homeTeamId(homeTeamId.toString()).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        // given
        given(matchService.updateMatch(
                eq(matchId),
                argThat(m -> m.getHomeTeamId().equals(homeTeamId.toString()))
        )).willThrow(new ResourceNotFoundException(Team.class, homeTeamId));

        // when
        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath(
                                "$.messages",
                                hasEntry("homeTeamId", List.of(String.format("team %s could not be found", homeTeamId)))
                        )
                );

    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 422 when away team not found")
    public void updateMatch_AwayTeamNotFound_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var awayTeamId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder()
                .awayTeamId(awayTeamId.toString()).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        // given
        given(matchService.updateMatch(
                eq(matchId),
                argThat(m -> m.getAwayTeamId().equals(awayTeamId.toString()))
        )).willThrow(new ResourceNotFoundException(Team.class, awayTeamId));

        // when
        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath(
                                "$.messages",
                                hasEntry("awayTeamId", List.of(String.format("team %s could not be found", awayTeamId)))
                        )
                );
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 422 when venue not found")
    public void updateMatch_VenueNotFound_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var venueId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder()
                .venueId(venueId.toString()).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        // given
        given(matchService.updateMatch(
                eq(matchId),
                argThat(m -> m.getVenueId().equals(venueId.toString()))
        )).willThrow(new ResourceNotFoundException(Venue.class, venueId));

        // when
        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath(
                                "$.messages",
                                hasEntry("venueId", List.of(String.format("venue %s could not be found", venueId)))
                        )
                );
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 422 when referee not found")
    public void updateMatch_RefereeNotFound_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();
        var refereeId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder()
                .refereeId(refereeId.toString()).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        // given
        given(matchService.updateMatch(
                eq(matchId),
                argThat(m -> m.getRefereeId().equals(refereeId.toString()))
        )).willThrow(new ResourceNotFoundException(Referee.class, refereeId));

        // when
        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath(
                                "$.messages",
                                hasEntry("refereeId", List.of(String.format("referee %s could not be found", refereeId)))
                        )
                );
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 200 and calls the service when request body valid")
    public void updateMatch_ValuesInBodyCorrect_StatusOkAndCallsService() throws Exception {
        var matchId = UUID.randomUUID();

        var expectedDto = TestMatchDto.builder()
                .startTimeUTC(LocalDateTime.of(2023, 1, 1, 19, 0))
                .build();
        var expectedJson = jsonMatchDto.write(expectedDto).getJson();

        var contentDto = TestUpsertMatchDto.builder()
                .homeTeamId(expectedDto.getHomeTeam().getId().toString())
                .startTimeUTC("2023/01/01 19:00")
                .awayTeamId(expectedDto.getAwayTeam().getId().toString())
                .venueId(expectedDto.getVenue().getId().toString())
                .refereeId(expectedDto.getReferee().getId().toString())
                .competitionId(expectedDto.getCompetitionId().toString())
                .build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();


        // given
        given(matchService.updateMatch(
                eq(matchId),
                argThat(m ->
                    m.getHomeTeamId().equals(contentDto.getHomeTeamId()) &&
                    m.getAwayTeamId().equals(contentDto.getAwayTeamId()) &&
                    m.getVenueId().equals(contentDto.getVenueId()) &&
                    m.getRefereeId().equals(contentDto.getRefereeId()) &&
                    m.getCompetitionId().equals(contentDto.getCompetitionId()) &&
                    m.getStartTimeUTC().equals(contentDto.getStartTimeUTC()))
        )).willReturn(expectedDto);

        // when
        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("DELETE /api/matches/:id returns 200 and a counter of how many matches have been deleted")
    public void deleteMatch_MatchIdProvided_StatusOkAndReturnsCounter() throws Exception {
        var id = UUID.randomUUID();

        // given
        given(matchService.markMatchAsDeleted(id)).willReturn(1);

        mvc.perform(
                        delete("/api/matches/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("{\"deleted\":1}"));
    }
}
