package ml.echelon133.matchservice.player.controller;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.player.dto.PlayerDto;
import ml.echelon133.common.team.dto.TeamDto;
import ml.echelon133.matchservice.MatchServiceApplication;
import ml.echelon133.matchservice.TestValidatorFactory;
import ml.echelon133.matchservice.country.constraints.CountryExists;
import ml.echelon133.matchservice.country.repository.CountryRepository;
import ml.echelon133.matchservice.player.TestPlayerDto;
import ml.echelon133.matchservice.player.TestUpsertPlayerDto;
import ml.echelon133.matchservice.player.model.Player;
import ml.echelon133.matchservice.player.model.UpsertPlayerDto;
import ml.echelon133.matchservice.player.service.PlayerService;
import ml.echelon133.matchservice.team.TestTeamDto;
import ml.echelon133.matchservice.team.service.TeamPlayerService;
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

import javax.validation.ConstraintValidator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class PlayerControllerTests {

    private MockMvc mvc;

    // used by @CountryExists.Validator
    @Mock
    private CountryRepository countryRepository;

    @Mock
    private PlayerService playerService;

    @Mock
    private TeamPlayerService teamPlayerService;

    @InjectMocks
    private PlayerExceptionHandler playerExceptionHandler;

    @InjectMocks
    private PlayerController playerController;

    private JacksonTester<PlayerDto> jsonPlayerDto;

    private JacksonTester<List<TeamDto>> jsonTeamDtos;

    private JacksonTester<UpsertPlayerDto> jsonUpsertPlayerDto;

    @BeforeEach
    public void beforeEach() {
        // validators with mocked dependencies which should be used by the standalone MockMvc configuration
        // every time a custom constraint validator is requested
        Map<Class<? extends ConstraintValidator>, ? extends ConstraintValidator> customValidators = Map.of(
                CountryExists.Validator.class, new CountryExists.Validator(countryRepository)
        );
        var validatorFactoryBean = TestValidatorFactory.getInstance(customValidators);

        // use a mapper with date/time modules, otherwise LocalDate won't work
        var om = MatchServiceApplication.objectMapper();

        JacksonTester.initFields(this, om);
        mvc = MockMvcBuilders.standaloneSetup(playerController)
                .setControllerAdvice(playerExceptionHandler)
                .setCustomArgumentResolvers(
                        // required while testing controller methods which use Pageable
                        new PageableHandlerMethodArgumentResolver()
                )
                .setValidator(validatorFactoryBean)
                .build();
    }

    @Test
    @DisplayName("GET /api/players/:id returns 404 when resource not found")
    public void getPlayerById_PlayerNotFound_StatusNotFound() throws Exception {
        var playerId = UUID.randomUUID();

        // given
        given(playerService.findById(playerId)).willThrow(
                new ResourceNotFoundException(Player.class, playerId)
        );

        mvc.perform(
                        get("/api/players/" + playerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("player %s could not be found", playerId)
                )));
    }

    @Test
    @DisplayName("GET /api/players/:id returns 200 and a valid entity if entity found")
    public void getPlayerById_PlayerFound_StatusOk() throws Exception {
        var playerId = UUID.randomUUID();

        var playerDto = TestPlayerDto.builder().build();
        var expectedJson = jsonPlayerDto.write(playerDto).getJson();

        // given
        given(playerService.findById(playerId)).willReturn(playerDto);

        mvc.perform(
                        get("/api/players/" + playerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("POST /api/players returns 422 when name is not provided")
    public void createPlayer_NameNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertPlayerDto.builder().name(null).build();
        var json = jsonUpsertPlayerDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/players")
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
    @DisplayName("POST /api/players returns 422 when name length is incorrect")
    public void createPlayer_NameLengthIncorrect_StatusUnprocessableEntity() throws Exception {
        var incorrectNameLengths = List.of(
                // too short (0 characters)
                "",
                /// too long (201 characters)
                "Xc9f0Gs7BSxW0SWDcEMz6vrM6e970ZQEB6LnTW3sIqtZwZOdcqAl2gvNvn2huYpPwCDnu7td5cFjAUXJaZDmaZ37oJSAKkTthS6hch6qhOJDQpvwISuXgLCHHrjl9VGRPInGCCla0yQ1ZkLVEYsjDUQ2RkrYFTi0wRvf75KXxvcLXqC7DoDGMPNpvvcy1Vh7WxaP3F4CQ"
        );

        for (String incorrectName : incorrectNameLengths) {
            var contentDto = TestUpsertPlayerDto.builder().name(incorrectName).build();
            var json = jsonUpsertPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/players")
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
    @DisplayName("POST /api/players returns 200 when name length is correct")
    public void createPlayer_NameLengthCorrect_StatusOk() throws Exception {
        var correctNameLengths = List.of(
                // minimum 1 character
                "a",
                /// maximum 200 characters
                "Xc9f0Gs7BSxW0SWDcEMz6vrM6e970ZQEB6LnTW3sIqtZwZOdcqAl2gvNvn2huYpPwCDnu7td5cFjAUXJaZDmaZ37oJSAKkTthS6hch6qhOJDQpvwISuXgLCHHrjl9VGRPInGCCla0yQ1ZkLVEYsjDUQ2RkrYFTi0wRvf75KXxvcLXqC7DoDGMPNpvvcy1Vh7WxaP3F4C"
        );

        // given
        // make sure that @CountryExists in UpsertPlayerDto is true
        given(countryRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

        // when
        for (String correctName : correctNameLengths) {
            var contentDto = TestUpsertPlayerDto.builder().name(correctName).build();
            var bodyJson = jsonUpsertPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/players")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(bodyJson)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/players returns 422 when countryId is not provided")
    public void createPlayer_CountryIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertPlayerDto.builder().countryId(null).build();
        var json = jsonUpsertPlayerDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/players")
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
    @DisplayName("POST /api/players returns 422 when countryId is not a uuid")
    public void createPlayer_CountryIdInvalidUuid_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertPlayerDto.builder().countryId("a").build();
        var json = jsonUpsertPlayerDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("countryId", List.of("id does not belong to a valid country")))
                );
    }

    @Test
    @DisplayName("POST /api/players returns 422 when the countryId does not reference an existing entity")
    public void createPlayer_CountryDoesNotExist_StatusUnprocessableEntity() throws Exception {
        var countryId = UUID.randomUUID();
        var contentDto = TestUpsertPlayerDto.builder().countryId(countryId.toString()).build();
        var json = jsonUpsertPlayerDto.write(contentDto).getJson();

        // given
        given(countryRepository.existsByIdAndDeletedFalse(countryId)).willReturn(false);

        // when
        mvc.perform(
                        post("/api/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("countryId", List.of("id does not belong to a valid country")))
                );
    }

    @Test
    @DisplayName("POST /api/players returns 422 when position is not provided")
    public void createPlayer_PositionNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertPlayerDto.builder().position(null).build();
        var json = jsonUpsertPlayerDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/players")
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
    @DisplayName("POST /api/players returns 422 when position value is incorrect")
    public void createPlayer_PositionIncorrect_StatusUnprocessableEntity() throws Exception {
        var incorrectPositions = List.of(
                "asdf", "", "TEST", "test", "aaaaaaaaaaaaaaaaaaaaaa"
        );

        for (String incorrectPosition : incorrectPositions) {
            var contentDto = TestUpsertPlayerDto.builder().position(incorrectPosition).build();
            var json = jsonUpsertPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/players")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.messages", hasEntry("position", List.of("required exactly one of [GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD]"))));
        }
    }

    @Test
    @DisplayName("POST /api/players returns 200 when position value is correct")
    public void createPlayer_PositionCorrect_StatusOk() throws Exception {
        var correctPositions = List.of(
                "GOALKEEPER", "DEFENDER", "MIDFIELDER", "FORWARD", // uppercase
                "goalkeeper", "defender", "midfielder", "forward", // lowercase
                "GOALkeeper", "DEFender", "MIDfielder", "FORward" // mixed
        );

        // given
        // make sure that @CountryExists in UpsertPlayerDto is true
        given(countryRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

        // when
        for (String correctPosition : correctPositions) {
            var contentDto = TestUpsertPlayerDto.builder().position(correctPosition).build();
            var json = jsonUpsertPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/players")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/players returns 422 when dateOfBirth is not provided")
    public void createPlayer_DateOfBirthNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = TestUpsertPlayerDto.builder().dateOfBirth(null).build();
        var json = jsonUpsertPlayerDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("dateOfBirth", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("POST /api/players returns 422 when dateOfBirth is incorrect")
    public void createPlayer_DateOfBirthIncorrect_StatusUnprocessableEntity() throws Exception {
        // any value that's not a date in yyyy/MM/dd format will be rejected
        var incorrectDates = List.of(
                "1970/01/01/01", "asdf", "", "01-01-1970", "01/01/1970", "1970-01-01"
        );

        for (String incorrectDate : incorrectDates) {
            var contentDto = TestUpsertPlayerDto.builder().dateOfBirth(incorrectDate).build();
            var json = jsonUpsertPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/players")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("dateOfBirth", List.of("required date format yyyy/MM/d")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/players returns 200 when dateOfBirth is correct")
    public void createPlayer_DateOfBirthCorrect_StatusOk() throws Exception {
        var correctDates = List.of(
                "1970/01/01", "1988/08/10", "2000/03/05"
        );

        // given
        // make sure that @CountryExists in UpsertPlayerDto is true
        given(countryRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

        // when
        for (String correctDate : correctDates) {
            var contentDto = TestUpsertPlayerDto.builder().dateOfBirth(correctDate).build();
            var json = jsonUpsertPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/players")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/players returns 200 and calls the service when request body valid")
    public void createPlayer_ValuesInBodyCorrect_StatusOkAndCallsService() throws Exception {
        var contentDto = TestUpsertPlayerDto.builder().build();
        var json = jsonUpsertPlayerDto.write(contentDto).getJson();

        var expectedDto = TestPlayerDto.builder()
                .name(contentDto.getName())
                .position(contentDto.getPosition())
                .dateOfBirth(LocalDate.parse(contentDto.getDateOfBirth(), DateTimeFormatter.ofPattern(PlayerService.DATE_OF_BIRTH_FORMAT)))
                .build();
        var expectedJson = jsonPlayerDto.write(expectedDto).getJson();

        // given
        // make sure that @CountryExists in UpsertPlayerDto is true
        given(countryRepository.existsByIdAndDeletedFalse(any())).willReturn(true);
        given(playerService.createPlayer(argThat(a ->
            a.getName().equals(contentDto.getName()) &&
                    a.getCountryId().equals(contentDto.getCountryId()) &&
                    a.getPosition().equals(contentDto.getPosition()) &&
                    a.getDateOfBirth().equals(contentDto.getDateOfBirth())
        ))).willReturn(expectedDto);

        mvc.perform(
                        post("/api/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("DELETE /api/players/:id returns 200 and a counter of how many players have been deleted")
    public void deletePlayer_PlayerIdProvided_StatusOkAndReturnsCounter() throws Exception {
        var id = UUID.randomUUID();

        // given
        given(playerService.markPlayerAsDeleted(id)).willReturn(1);

        mvc.perform(
                        delete("/api/players/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("{\"deleted\":1}"));
    }

    @Test
    @DisplayName("PUT /api/players/:id returns 404 when resource not found")
    public void updatePlayer_PlayerNotFound_StatusNotFound() throws Exception {
        var playerId = UUID.randomUUID();
        var upsertDto = TestUpsertPlayerDto.builder().build();
        var upsertJson = jsonUpsertPlayerDto.write(upsertDto).getJson();

        // given
        // make sure that @CountryExists in UpsertPlayerDto is true
        given(countryRepository.existsByIdAndDeletedFalse(any())).willReturn(true);
        given(playerService.updatePlayer(eq(playerId), any())).willThrow(
                new ResourceNotFoundException(Player.class, playerId)
        );

        mvc.perform(
                        put("/api/players/" + playerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(upsertJson)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("player %s could not be found", playerId)
                )));
    }

    @Test
    @DisplayName("PUT /api/players/:id returns 422 when name is not provided")
    public void updatePlayer_NameNotProvided_StatusUnprocessableEntity() throws Exception {
        var playerId = UUID.randomUUID();
        var contentDto = TestUpsertPlayerDto.builder().name(null).build();
        var json = jsonUpsertPlayerDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/players/" + playerId)
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
    @DisplayName("PUT /api/players/:id returns 422 when name length is incorrect")
    public void updatePlayer_NameLengthIncorrect_StatusUnprocessableEntity() throws Exception {
        var playerId = UUID.randomUUID();
        var incorrectNameLengths = List.of(
                // too short (0 characters)
                "",
                /// too long (201 characters)
                "Xc9f0Gs7BSxW0SWDcEMz6vrM6e970ZQEB6LnTW3sIqtZwZOdcqAl2gvNvn2huYpPwCDnu7td5cFjAUXJaZDmaZ37oJSAKkTthS6hch6qhOJDQpvwISuXgLCHHrjl9VGRPInGCCla0yQ1ZkLVEYsjDUQ2RkrYFTi0wRvf75KXxvcLXqC7DoDGMPNpvvcy1Vh7WxaP3F4CQ"
        );

        for (String incorrectName : incorrectNameLengths) {
            var contentDto = TestUpsertPlayerDto.builder().name(incorrectName).build();
            var json = jsonUpsertPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/players/" + playerId)
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
    @DisplayName("PUT /api/players/:id returns 200 when name length is correct")
    public void updatePlayer_NameLengthCorrect_StatusOk() throws Exception {
        var playerId = UUID.randomUUID();
        var correctNameLengths = List.of(
                // minimum 1 character
                "a",
                /// maximum 200 characters
                "Xc9f0Gs7BSxW0SWDcEMz6vrM6e970ZQEB6LnTW3sIqtZwZOdcqAl2gvNvn2huYpPwCDnu7td5cFjAUXJaZDmaZ37oJSAKkTthS6hch6qhOJDQpvwISuXgLCHHrjl9VGRPInGCCla0yQ1ZkLVEYsjDUQ2RkrYFTi0wRvf75KXxvcLXqC7DoDGMPNpvvcy1Vh7WxaP3F4C"
        );

        // given
        // make sure that @CountryExists in UpsertPlayerDto is true
        given(countryRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

        // when
        for (String correctName : correctNameLengths) {
            var contentDto = TestUpsertPlayerDto.builder().name(correctName).build();
            var bodyJson = jsonUpsertPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/players/" + playerId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(bodyJson)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("PUT /api/players/:id returns 422 when countryId is not provided")
    public void updatePlayer_CountryIdNotProvided_StatusUnprocessableEntity() throws Exception {
        var playerId = UUID.randomUUID();
        var contentDto = TestUpsertPlayerDto.builder().countryId(null).build();
        var json = jsonUpsertPlayerDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/players/" + playerId)
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
    @DisplayName("PUT /api/players/:id returns 422 when countryId is not a uuid")
    public void updatePlayer_CountryIdInvalidUuid_StatusUnprocessableEntity() throws Exception {
        var playerId = UUID.randomUUID();
        var contentDto = TestUpsertPlayerDto.builder().countryId("a").build();
        var json = jsonUpsertPlayerDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/players/" + playerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("countryId", List.of("id does not belong to a valid country")))
                );
    }

    @Test
    @DisplayName("PUT /api/players/:id returns 422 when the countryId does not reference an existing entity")
    public void updatePlayer_CountryDoesNotExist_StatusUnprocessableEntity() throws Exception {
        var countryId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var contentDto = TestUpsertPlayerDto.builder().countryId(countryId.toString()).build();
        var json = jsonUpsertPlayerDto.write(contentDto).getJson();

        // given
        given(countryRepository.existsByIdAndDeletedFalse(countryId)).willReturn(false);

        // when
        mvc.perform(
                        put("/api/players/" + playerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("countryId", List.of("id does not belong to a valid country")))
                );
    }

    @Test
    @DisplayName("PUT /api/players/:id returns 422 when position is not provided")
    public void updatePlayer_PositionNotProvided_StatusUnprocessableEntity() throws Exception {
        var playerId = UUID.randomUUID();
        var contentDto = TestUpsertPlayerDto.builder().position(null).build();
        var json = jsonUpsertPlayerDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/players/" + playerId)
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
    @DisplayName("PUT /api/players/:id returns 422 when position value is incorrect")
    public void updatePlayer_PositionIncorrect_StatusUnprocessableEntity() throws Exception {
        var playerId = UUID.randomUUID();
        var incorrectPositions = List.of(
                "asdf", "", "TEST", "test", "aaaaaaaaaaaaaaaaaaaaaa"
        );

        for (String incorrectPosition : incorrectPositions) {
            var contentDto = TestUpsertPlayerDto.builder().position(incorrectPosition).build();
            var json = jsonUpsertPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/players/" + playerId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.messages", hasEntry("position", List.of("required exactly one of [GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD]"))));
        }
    }

    @Test
    @DisplayName("PUT /api/players/:id returns 200 when position value is correct")
    public void updatePlayer_PositionCorrect_StatusOk() throws Exception {
        var playerId = UUID.randomUUID();
        var correctPositions = List.of(
                "GOALKEEPER", "DEFENDER", "MIDFIELDER", "FORWARD", // uppercase
                "goalkeeper", "defender", "midfielder", "forward", // lowercase
                "GOALkeeper", "DEFender", "MIDfielder", "FORward" // mixed
        );

        // given
        // make sure that @CountryExists in UpsertPlayerDto is true
        given(countryRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

        // when
        for (String correctPosition : correctPositions) {
            var contentDto = TestUpsertPlayerDto.builder().position(correctPosition).build();
            var json = jsonUpsertPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/players/" + playerId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("PUT /api/players/:id returns 422 when dateOfBirth is not provided")
    public void updatePlayer_DateOfBirthNotProvided_StatusUnprocessableEntity() throws Exception {
        var playerId = UUID.randomUUID();
        var contentDto = TestUpsertPlayerDto.builder().dateOfBirth(null).build();
        var json = jsonUpsertPlayerDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/players/" + playerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("dateOfBirth", List.of("field has to be provided")))
                );
    }

    @Test
    @DisplayName("PUT /api/players/:id returns 422 when dateOfBirth is incorrect")
    public void updatePlayer_DateOfBirthIncorrect_StatusUnprocessableEntity() throws Exception {
        var playerId = UUID.randomUUID();
        // any value that's not a date in yyyy/MM/dd format will be rejected
        var incorrectDates = List.of(
                "1970/01/01/01", "asdf", "", "01-01-1970", "01/01/1970", "1970-01-01"
        );

        for (String incorrectDate : incorrectDates) {
            var contentDto = TestUpsertPlayerDto.builder().dateOfBirth(incorrectDate).build();
            var json = jsonUpsertPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/players/" + playerId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("dateOfBirth", List.of("required date format yyyy/MM/d")))
                    );
        }
    }

    @Test
    @DisplayName("PUT /api/players/:id returns 200 when dateOfBirth is correct")
    public void updatePlayer_DateOfBirthCorrect_StatusOk() throws Exception {
        var playerId = UUID.randomUUID();
        var correctDates = List.of(
                "1970/01/01", "1988/08/10", "2000/03/05"
        );

        // given
        // make sure that @CountryExists in UpsertPlayerDto is true
        given(countryRepository.existsByIdAndDeletedFalse(any())).willReturn(true);

        // when
        for (String correctDate : correctDates) {
            var contentDto = TestUpsertPlayerDto.builder().dateOfBirth(correctDate).build();
            var json = jsonUpsertPlayerDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/players/" + playerId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("PUT /api/players/:id returns 200 and calls the service when request body valid")
    public void updatePlayer_ValuesInBodyCorrect_StatusOkAndCallsService() throws Exception {
        var playerId = UUID.randomUUID();
        var contentDto = TestUpsertPlayerDto.builder().build();
        var json = jsonUpsertPlayerDto.write(contentDto).getJson();

        var expectedDto = TestPlayerDto.builder()
                .name(contentDto.getName())
                .position(contentDto.getPosition())
                .dateOfBirth(LocalDate.parse(contentDto.getDateOfBirth(), DateTimeFormatter.ofPattern(PlayerService.DATE_OF_BIRTH_FORMAT)))
                .build();
        var expectedJson = jsonPlayerDto.write(expectedDto).getJson();

        // given
        // make sure that @CountryExists in UpsertPlayerDto is true
        given(countryRepository.existsByIdAndDeletedFalse(any())).willReturn(true);
        given(playerService.updatePlayer(
                eq(playerId),
                argThat(a ->
                a.getName().equals(contentDto.getName()) &&
                        a.getCountryId().equals(contentDto.getCountryId()) &&
                        a.getPosition().equals(contentDto.getPosition()) &&
                        a.getDateOfBirth().equals(contentDto.getDateOfBirth())
        ))).willReturn(expectedDto);

        // when
        mvc.perform(
                        put("/api/players/" + playerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("GET /api/players?name= returns 400 when `name` is not provided")
    public void getPlayersByName_NameNotProvided_StatusBadRequest() throws Exception {
        mvc.perform(
                        get("/api/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]", is("query parameter 'name' not provided")));
    }

    @Test
    @DisplayName("GET /api/players?name returns 200 when `name` is provided and pageable is default")
    public void getPlayersByName_NameProvidedWithDefaultPageable_StatusOk() throws Exception {
        var pValue = "test";
        var defaultPageNumber = 0;
        var defaultPageSize = 20;

        Page<PlayerDto> expectedPage = Page.empty();

        //given
        given(playerService.findPlayersByName(
                eq(pValue),
                argThat(p -> p.getPageSize() == defaultPageSize && p.getPageNumber() == defaultPageNumber)
        )).willReturn(expectedPage);

        mvc.perform(
                        get("/api/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("name", pValue)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(0)));
    }

    @Test
    @DisplayName("GET /api/players?name returns 200 when `name` is provided and pageable values are custom")
    public void getPlayersByName_NameProvidedWithCustomPageParameters_StatusOk() throws Exception {
        var pValue = "test";
        var testPageSize = 7;
        var testPageNumber = 4;
        var expectedPageable = Pageable.ofSize(testPageSize).withPage(testPageNumber);
        var expectedContent = List.of(TestPlayerDto.builder().name(pValue).build());

        Page<PlayerDto> expectedPage = new PageImpl<>(expectedContent, expectedPageable, 1);

        //given
        given(playerService.findPlayersByName(
                eq(pValue),
                argThat(p -> p.getPageNumber() == testPageNumber && p.getPageSize() == testPageSize)
        )).willReturn(expectedPage);

        mvc.perform(
                        get("/api/players")
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
    @DisplayName("GET /api/players/:id/teams returns 200 when player's teams are found")
    public void getTeamsOfPlayer_TeamsFound_StatusOk() throws Exception {
        var playerId = UUID.randomUUID();
        var teamDtos = List.of(
                TestTeamDto.builder().build()
        );

        var expectedJson = jsonTeamDtos.write(teamDtos).getJson();

        // given
        given(teamPlayerService.findAllTeamsOfPlayer(playerId)).willReturn(teamDtos);

        // when
        mvc.perform(
                        get("/api/players/" + playerId + "/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }
}
