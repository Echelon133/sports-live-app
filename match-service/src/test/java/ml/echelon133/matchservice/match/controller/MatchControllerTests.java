package ml.echelon133.matchservice.match.controller;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.match.dto.CompactMatchDto;
import ml.echelon133.common.match.dto.MatchDto;
import ml.echelon133.matchservice.MatchServiceApplication;
import ml.echelon133.matchservice.TestValidatorFactory;
import ml.echelon133.matchservice.match.TestMatchDto;
import ml.echelon133.matchservice.match.TestUpsertMatchDto;
import ml.echelon133.matchservice.match.model.Match;
import ml.echelon133.matchservice.match.model.UpsertMatchDto;
import ml.echelon133.matchservice.match.service.MatchService;
import ml.echelon133.matchservice.referee.constraints.RefereeExists;
import ml.echelon133.matchservice.referee.repository.RefereeRepository;
import ml.echelon133.matchservice.team.constraints.TeamExists;
import ml.echelon133.matchservice.team.repository.TeamRepository;
import ml.echelon133.matchservice.venue.constraints.VenueExists;
import ml.echelon133.matchservice.venue.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.validation.ConstraintValidator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class MatchControllerTests {

    private MockMvc mvc;

    // used by @TeamExists.Validator
    @Mock
    private TeamRepository teamRepository;

    // used by @VenueExists.Validator
    @Mock
    private VenueRepository venueRepository;

    // used by @RefereeExists.Validator
    @Mock
    private RefereeRepository refereeRepository;

    @Mock
    private MatchService matchService;

    @InjectMocks
    private MatchExceptionHandler matchExceptionHandler;

    @InjectMocks
    private MatchController matchController;

    private JacksonTester<MatchDto> jsonMatchDto;

    private JacksonTester<UpsertMatchDto> jsonUpsertMatchDto;

    @BeforeEach
    public void beforeEach() {
        // validators with mocked dependencies which should be used by the standalone MockMvc configuration
        // every time a custom constraint validator is requested
        Map<Class<? extends ConstraintValidator>, ? extends ConstraintValidator> customValidators = Map.of(
                TeamExists.Validator.class, new TeamExists.Validator(teamRepository),
                VenueExists.Validator.class, new VenueExists.Validator(venueRepository),
                RefereeExists.Validator.class, new RefereeExists.Validator(refereeRepository)
        );
        var validatorFactoryBean = TestValidatorFactory.getInstance(customValidators);

        // use a mapper with date/time modules, otherwise LocalDate won't work
        var om = MatchServiceApplication.objectMapper();

        JacksonTester.initFields(this, om);
        mvc = MockMvcBuilders
                .standaloneSetup(matchController)
                .setControllerAdvice(matchExceptionHandler)
                .setCustomArgumentResolvers(
                        // required while testing controller methods which use Pageable
                        new PageableHandlerMethodArgumentResolver()
                )
                .setValidator(validatorFactoryBean)
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
                        jsonPath("$.messages", hasEntry("homeTeamId", List.of("id does not belong to a valid team")))
                );
    }

    @Test
    @DisplayName("POST /api/matches returns 422 when the homeTeamId does not reference an existing entity")
    public void createMatch_HomeTeamDoesNotExist_StatusUnprocessableEntity() throws Exception {
        var homeTeamId = UUID.randomUUID();
        var contentDto = TestUpsertMatchDto.builder().homeTeamId(homeTeamId.toString()).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        // given
        given(teamRepository.existsByIdAndDeletedFalse(homeTeamId)).willReturn(false);

        // when
        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("homeTeamId", List.of("id does not belong to a valid team")))
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
                        jsonPath("$.messages", hasEntry("awayTeamId", List.of("id does not belong to a valid team")))
                );
    }

    @Test
    @DisplayName("POST /api/matches returns 422 when the awayTeamId does not reference an existing entity")
    public void createMatch_AwayTeamDoesNotExist_StatusUnprocessableEntity() throws Exception {
        var awayTeamId = UUID.randomUUID();
        var contentDto = TestUpsertMatchDto.builder().awayTeamId(awayTeamId.toString()).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        // given
        given(teamRepository.existsByIdAndDeletedFalse(any())).willReturn(false);

        // when
        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("awayTeamId", List.of("id does not belong to a valid team")))
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
                        jsonPath("$.messages", hasEntry("venueId", List.of("id does not belong to a valid venue")))
                );
    }

    @Test
    @DisplayName("POST /api/matches returns 422 when the venueId does not reference an existing entity")
    public void createMatch_VenueDoesNotExist_StatusUnprocessableEntity() throws Exception {
        var venueId = UUID.randomUUID();
        var contentDto = TestUpsertMatchDto.builder().venueId(venueId.toString()).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        // given
        given(venueRepository.existsByIdAndDeletedFalse(venueId)).willReturn(false);

        // when
        mvc.perform(
                        post("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("venueId", List.of("id does not belong to a valid venue")))
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

        // given
        given(teamRepository.existsByIdAndDeletedFalse(any())).willReturn(true);
        given(venueRepository.existsByIdAndDeletedFalse(any())).willReturn(true);


        // when
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
                        jsonPath("$.messages", hasEntry("refereeId", List.of("id does not belong to a valid referee")))
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
        given(teamRepository.existsByIdAndDeletedFalse(any())).willReturn(true);
        given(venueRepository.existsByIdAndDeletedFalse(any())).willReturn(true);
        given(refereeRepository.existsByIdAndDeletedFalse(any())).willReturn(true);
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
                        jsonPath("$.messages", hasEntry("homeTeamId", List.of("id does not belong to a valid team")))
                );
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 422 when the homeTeamId does not reference an existing entity")
    public void updateMatch_HomeTeamDoesNotExist_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();

        var homeTeamId = UUID.randomUUID();
        var contentDto = TestUpsertMatchDto.builder().homeTeamId(homeTeamId.toString()).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        // given
        given(teamRepository.existsByIdAndDeletedFalse(homeTeamId)).willReturn(false);

        // when
        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("homeTeamId", List.of("id does not belong to a valid team")))
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
                        jsonPath("$.messages", hasEntry("awayTeamId", List.of("id does not belong to a valid team")))
                );
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 422 when the awayTeamId does not reference an existing entity")
    public void updateMatch_AwayTeamDoesNotExist_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();

        var awayTeamId = UUID.randomUUID();
        var contentDto = TestUpsertMatchDto.builder().awayTeamId(awayTeamId.toString()).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        // given
        given(teamRepository.existsByIdAndDeletedFalse(any())).willReturn(false);

        // when
        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("awayTeamId", List.of("id does not belong to a valid team")))
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
                        jsonPath("$.messages", hasEntry("venueId", List.of("id does not belong to a valid venue")))
                );
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 422 when the venueId does not reference an existing entity")
    public void updateMatch_VenueDoesNotExist_StatusUnprocessableEntity() throws Exception {
        var matchId = UUID.randomUUID();

        var venueId = UUID.randomUUID();
        var contentDto = TestUpsertMatchDto.builder().venueId(venueId.toString()).build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        // given
        given(venueRepository.existsByIdAndDeletedFalse(venueId)).willReturn(false);

        // when
        mvc.perform(
                        put("/api/matches/" + matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("venueId", List.of("id does not belong to a valid venue")))
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

        // given
        given(teamRepository.existsByIdAndDeletedFalse(any())).willReturn(true);
        given(venueRepository.existsByIdAndDeletedFalse(any())).willReturn(true);


        // when
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
                        jsonPath("$.messages", hasEntry("refereeId", List.of("id does not belong to a valid referee")))
                );
    }

    @Test
    @DisplayName("PUT /api/matches/:id returns 404 when match not found")
    public void updateMatch_MatchNotFound_StatusNotFound() throws Exception {
        var matchId = UUID.randomUUID();

        var contentDto = TestUpsertMatchDto.builder().build();
        var json = jsonUpsertMatchDto.write(contentDto).getJson();

        // given
        given(teamRepository.existsByIdAndDeletedFalse(any())).willReturn(true);
        given(venueRepository.existsByIdAndDeletedFalse(any())).willReturn(true);
        given(refereeRepository.existsByIdAndDeletedFalse(any())).willReturn(true);
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
        given(teamRepository.existsByIdAndDeletedFalse(any())).willReturn(true);
        given(venueRepository.existsByIdAndDeletedFalse(any())).willReturn(true);
        given(refereeRepository.existsByIdAndDeletedFalse(any())).willReturn(true);
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

    @Test
    @DisplayName("GET /api/matches returns 400 when both `competitionId` and `date` are not provided")
    public void getMatchesByCriteria_BothKeyRequestParamsNotProvided_StatusBadRequest() throws Exception {

        mvc.perform(
                    get("/api/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasItems(
                        "query parameter 'competitionId' not provided",
                        "query parameter 'date' not provided"
                )));
    }

    @Test
    @DisplayName("GET /api/matches returns 400 when mutually exclusive `competitionId` and `date` are provided at the same time")
    public void getMatchesByCriteria_MutuallyExclusiveRequestParamsProvided_StatusBadRequest() throws Exception {

        mvc.perform(
                        get("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("competitionId", "test")
                                .param("date", "asdf")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasItems(
                        "query parameter 'competitionId' cannot be provided together with 'date'",
                        "query parameter 'date' cannot be provided together with 'competitionId'"
                )));
    }

    @Test
    @DisplayName("GET /api/matches returns 400 when provided `date` has an incorrect format")
    public void getMatchesByCriteria_IncorrectDateProvided_StatusBadRequest() throws Exception {
        var incorrectDates = List.of("2023-01-01", "01/01/23", "2023/01/01 20:00");

        for (String incorrectDate : incorrectDates) {
            mvc.perform(
                            get("/api/matches")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .param("date", incorrectDate)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messages", hasItem(
                            "query parameter 'date' format should be yyyy/mm/dd"
                    )));
        }
    }

    @Test
    @DisplayName("GET /api/matches returns 200 and sets default `utcOffset` value when valid `date` is provided")
    public void getMatchesByCriteria_ValidDateProvided_StatusOkAndDefaultUtcOffsetSet() throws Exception {
        var defaultUtcOffset = ZoneOffset.UTC;

        mvc.perform(
                        get("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("date", "2023/01/01")
                )
                .andExpect(status().isOk());

        verify(matchService).findMatchesByDate(
                eq(LocalDate.of(2023, 1, 1)),
                eq(defaultUtcOffset),
                eq(Pageable.ofSize(20).withPage(0))
        );
    }

    @Test
    @DisplayName("GET /api/matches returns 400 when `date` is provided and provided `utcOffset` has an incorrect format")
    public void getMatchesByCriteria_DateProvidedAndIncorrectUtcOffsetFormat_StatusBadRequest() throws Exception {
        var incorrectUtcOffsets = List.of("asdf", "+-000:000", "+AA:BB", "-25:99");

        for (String incorrectUtcOffset: incorrectUtcOffsets) {
            mvc.perform(
                            get("/api/matches")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .param("date", "2023/01/01")
                                    .param("utcOffset", incorrectUtcOffset)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messages", hasItem(
                            "query parameter 'utcOffset' format should be ±hh:mm"
                    )));
        }
    }

    @Test
    @DisplayName("GET /api/matches returns 200 when `date` is provided and `utcOffset` contains positive or negative offsets")
    public void getMatchesByCriteria_DateAndUtcOffsetProvided_StatusOk() throws Exception {
        var correctUtcOffsets = List.of("+00:00", "+05:59", "-08:30", "-01:00");

        for (String correctUtcOffset : correctUtcOffsets) {
            var competitionId = UUID.randomUUID();
            var matches = List.of(CompactMatchDto.builder().build());

            // given
            doReturn(Map.of(competitionId, matches)).when(matchService).findMatchesByDate(
                eq(LocalDate.of(2023, 1, 1)),
                eq(ZoneOffset.of(correctUtcOffset)),
                eq(Pageable.ofSize(20).withPage(0))
            );

            // when
            var path = "$." + competitionId;
            mvc.perform(
                            get("/api/matches")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .param("date", "2023/01/01")
                                    .param("utcOffset", correctUtcOffset)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(path, hasSize(1)));
        }
    }

    @Test
    @DisplayName("GET /api/matches returns 200 when the values of `date` and `utcOffset` are valid but the keys use irregular case")
    public void getMatchesByCriteria_DateAndUtcOffsetKeysWithIrregularCase_StatusOk() throws Exception {
        mvc.perform(
                        get("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("DaTe", "2023/01/01")
                                .param("UtCoFfSeT", "+02:00")
                )
                .andExpect(status().isOk());

        verify(matchService).findMatchesByDate(
                eq(LocalDate.of(2023, 1, 1)),
                eq(ZoneOffset.of("+02:00")),
                eq(Pageable.ofSize(20).withPage(0))
        );
    }

    @Test
    @DisplayName("GET /api/matches returns 200 when custom `page` and `size` are provided alongside `date` and `utcOffset`")
    public void getMatchesByCriteria_DateProvidedAndCustomPageAndSize_StatusOkAndUsesCustomPageable() throws Exception {
        mvc.perform(
                        get("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("date", "2023/01/01")
                                .param("utcOffset", "+02:00")
                                .param("page", "2")
                                .param("size", "35")
                )
                .andExpect(status().isOk());

        verify(matchService).findMatchesByDate(
                eq(LocalDate.of(2023, 1, 1)),
                eq(ZoneOffset.of("+02:00")),
                eq(Pageable.ofSize(35).withPage(2))
        );
    }

    @Test
    @DisplayName("GET /api/matches returns 400 when `competitionId` is provided but `type` is not")
    public void getMatchesByCriteria_CompetitionIdProvidedAndTypeNotProvided_StatusBadRequest() throws Exception {
        mvc.perform(
                        get("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("competitionId", UUID.randomUUID().toString())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasItem(
                    "query parameter 'type' not provided"
                )));
    }

    @Test
    @DisplayName("GET /api/matches returns 400 when `competitionId` is not an uuid")
    public void getMatchesByCriteria_InvalidCompetitionId_StatusBadRequest() throws Exception {
        mvc.perform(
                        get("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("competitionId", "asdf")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasItem(
                        "query parameter 'competitionId' not a uuid"
                )));
    }

    @Test
    @DisplayName("GET /api/matches returns 400 when `competitionId` is provided and `type` is incorrect")
    public void getMatchesByCriteria_CompetitionIdProvidedAndTypeIncorrect_StatusBadRequest() throws Exception {
        mvc.perform(
                        get("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("competitionId", UUID.randomUUID().toString())
                                .param("type", "ADSDF")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasItem(
                        "query parameter 'type' should be either 'fixtures' or 'results'"
                )));
    }

    @Test
    @DisplayName("GET /api/matches returns 200 when `competitionId` is provided and `type` is 'results'")
    public void getMatchesByCriteria_CompetitionIdProvidedAndTypeIsResults_StatusOkAndCorrectlyCallsService() throws Exception {
        var competitionId = UUID.randomUUID();

        // given
        var matches = List.of(CompactMatchDto.builder().build());
        given(matchService.findMatchesByCompetition(
                eq(competitionId),
                eq(true),
                eq(Pageable.ofSize(20).withPage(0))
        )).willReturn(Map.of(competitionId, matches));

        // when
        var path = "$." + competitionId;
        mvc.perform(
                        get("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("competitionId", competitionId.toString())
                                .param("type", "results")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(path, hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/matches returns 200 when `competitionId` is provided and `type` is 'fixtures'")
    public void getMatchesByCriteria_CompetitionIdProvidedAndTypeIsFixtures_StatusOkAndCorrectlyCallsService() throws Exception {
        var competitionId = UUID.randomUUID();

        // given
        var matches = List.of(CompactMatchDto.builder().build());
        given(matchService.findMatchesByCompetition(
                eq(competitionId),
                eq(false),
                eq(Pageable.ofSize(20).withPage(0))
        )).willReturn(Map.of(competitionId, matches));

        // when
        var path = "$." + competitionId;
        mvc.perform(
                        get("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("competitionId", competitionId.toString())
                                .param("type", "fixtures")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(path, hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/matches returns 200 when custom `page` and `size` are provided alongside `competitionId` and `type`")
    public void getMatchesByCriteria_CompetitionIdProvidedAndCustomPageAndSize_StatusOkAndUsesCustomPageable() throws Exception {
        var competitionId = UUID.randomUUID();
        mvc.perform(
                        get("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("competitionId", competitionId.toString())
                                .param("type", "fixtures")
                                .param("page", "4")
                                .param("size", "50")
                )
                .andExpect(status().isOk());

        verify(matchService).findMatchesByCompetition(
                eq(competitionId),
                eq(false),
                eq(Pageable.ofSize(50).withPage(4))
        );
    }
}
