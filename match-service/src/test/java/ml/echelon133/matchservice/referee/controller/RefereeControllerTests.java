package ml.echelon133.matchservice.referee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.matchservice.referee.model.RefereeDto;
import ml.echelon133.matchservice.referee.model.UpsertRefereeDto;
import ml.echelon133.matchservice.referee.service.RefereeService;
import ml.echelon133.matchservice.referee.model.Referee;
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
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ExtendWith(MockitoExtension.class)
public class RefereeControllerTests {

    private MockMvc mvc;

    @Mock
    private RefereeService refereeService;

    @InjectMocks
    private RefereeExceptionHandler refereeExceptionHandler;

    @InjectMocks
    private RefereeController refereeController;

    private JacksonTester<RefereeDto> jsonRefereeDto;

    private JacksonTester<UpsertRefereeDto> jsonUpsertRefereeDto;


    @BeforeEach
    public void beforeEach() {
        JacksonTester.initFields(this, new ObjectMapper());
        mvc = MockMvcBuilders.standaloneSetup(refereeController)
                .setControllerAdvice(refereeExceptionHandler)
                .setCustomArgumentResolvers(
                        // required while testing controller methods which use Pageable
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    @DisplayName("GET /api/referees/:id returns 404 when resource not found")
    public void getRefereeById_RefereeNotFound_StatusNotFound() throws Exception {
        var testId = UUID.randomUUID();

        // given
        given(refereeService.findById(testId)).willThrow(
                new ResourceNotFoundException(Referee.class, testId)
        );

        mvc.perform(
                        get("/api/referees/" + testId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("referee %s could not be found", testId)
                )));
    }

    @Test
    @DisplayName("GET /api/referees/:id returns 200 and a valid entity if entity found")
    public void getRefereeById_RefereeFound_StatusOk() throws Exception {
        var testId = UUID.randomUUID();
        var refereeDto = RefereeDto.from(testId, "Test");

        var expectedJson = jsonRefereeDto.write(refereeDto).getJson();

        // given
        given(refereeService.findById(testId)).willReturn(refereeDto);

        mvc.perform(
                        get("/api/referees/" + testId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("POST /api/referees returns 422 when name is not provided")
    public void createReferee_NameNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = new UpsertRefereeDto(null);
        var json = jsonUpsertRefereeDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/referees")
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
    @DisplayName("POST /api/referees returns 422 when name length is incorrect")
    public void createReferee_NameLengthIncorrect_StatusUnprocessableEntity() throws Exception {
        var incorrectNameLengths = List.of(
                // too short (0 characters)
                "",
                /// too long (101 characters)
                "6EUxnEDmbw2qPIeUhhjnVrLJbnjdslg8dp1HUac4oK7tzkIo9QkQ125QTTT6oPmzk7pfo28BrGINDzbXBYZdKGTMhSEtOGf1g6wgb"
        );

        for (String incorrectName : incorrectNameLengths) {
            var contentDto = new UpsertRefereeDto(incorrectName);
            var json = jsonUpsertRefereeDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/referees")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("name", List.of("expected length between 1 and 100")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/referees returns 200 when name length is correct")
    public void createReferee_NameLengthIsCorrect_StatusOk() throws Exception {
        var correctNameLengths = List.of(
                // 1 character (lower limit)
                "a",
                // 100 characters (upper limit)
                "6EUxnEDmbw2qPIeUhhjnVrLJbnjdslg8dp1HUac4oK7tzkIo9QkQ125QTTT6oPmzk7pfo28BrGINDzbXBYZdKGTMhSEtOGf1g6wg"
        );

        for (String correctName : correctNameLengths) {
            // expected from the database
            var dto = RefereeDto.from(UUID.randomUUID(), correctName);
            var expectedJson = jsonRefereeDto.write(dto).getJson();

            // what is given in the request body
            var contentDto = new UpsertRefereeDto(correctName);
            var bodyJson = jsonUpsertRefereeDto.write(contentDto).getJson();

            // use doReturn, because regular given/when does not work when re-declaring a single argThat matcher
            doReturn(dto).when(refereeService).createReferee(argThat(v -> v.name().equals(contentDto.name())));

            mvc.perform(
                            post("/api/referees")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(bodyJson)
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().string(expectedJson));
        }
    }

    @Test
    @DisplayName("DELETE /api/referees/:id returns 200 and a counter of how many referees have been deleted")
    public void deleteReferee_RefereeIdProvided_StatusOkAndReturnsCounter() throws Exception {
        var id = UUID.randomUUID();

        // given
        given(refereeService.markRefereeAsDeleted(id)).willReturn(1);

        mvc.perform(
                        delete("/api/referees/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("{\"deleted\":1}"));
    }

    @Test
    @DisplayName("PUT /api/referees/:id returns 404 when resource not found")
    public void updateReferee_RefereeNotFound_StatusNotFound() throws Exception {
        var testId = UUID.randomUUID();
        var upsertDto = new UpsertRefereeDto("Test");
        var upsertJson = jsonUpsertRefereeDto.write(upsertDto).getJson();

        // given
        given(refereeService.updateReferee(eq(testId), ArgumentMatchers.any())).willThrow(
                new ResourceNotFoundException(Referee.class, testId)
        );

        mvc.perform(
                        put("/api/referees/" + testId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(upsertJson)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("referee %s could not be found", testId)
                )));
    }

    @Test
    @DisplayName("PUT /api/referees/:id returns 422 when name is not provided")
    public void updateReferee_RefereeFoundAndNameNotProvided_StatusUnprocessableEntity() throws Exception {
        var testId = UUID.randomUUID();
        var contentDto = new UpsertRefereeDto(null);
        var json = jsonUpsertRefereeDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/referees/" + testId)
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
    @DisplayName("PUT /api/referees/:id returns 422 when name length is incorrect")
    public void updateReferee_RefereeFoundAndNameLengthIncorrect_StatusUnprocessableEntity() throws Exception {
        var testId = UUID.randomUUID();
        var incorrectNameLengths = List.of(
                // too short (0 characters)
                "",
                /// too long (101 characters)
                "6EUxnEDmbw2qPIeUhhjnVrLJbnjdslg8dp1HUac4oK7tzkIo9QkQ125QTTT6oPmzk7pfo28BrGINDzbXBYZdKGTMhSEtOGf1g6wgb"
        );

        for (String incorrectName : incorrectNameLengths) {
            var contentDto = new UpsertRefereeDto(incorrectName);
            var json = jsonUpsertRefereeDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/referees/" + testId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("name", List.of("expected length between 1 and 100")))
                    );
        }
    }

    @Test
    @DisplayName("PUT /api/referees/:id returns 200 when name length is correct")
    public void updateReferee_RefereeFoundAndNameLengthIsCorrect_StatusOk() throws Exception {
        var id = UUID.randomUUID();

        var correctNameLengths = List.of(
                // 1 character (lower limit)
                "a",
                // 100 characters (upper limit)
                "6EUxnEDmbw2qPIeUhhjnVrLJbnjdslg8dp1HUac4oK7tzkIo9QkQ125QTTT6oPmzk7pfo28BrGINDzbXBYZdKGTMhSEtOGf1g6wg"
        );

        for (String correctName : correctNameLengths) {
            // expected from the database
            var dto = RefereeDto.from(UUID.randomUUID(), correctName);
            var expectedJson = jsonRefereeDto.write(dto).getJson();

            // what is given in the request body
            var contentDto = new UpsertRefereeDto(correctName);
            var bodyJson = jsonUpsertRefereeDto.write(contentDto).getJson();

            // use doReturn, because regular given/when does not work when re-declaring a single argThat matcher
            doReturn(dto).when(refereeService).updateReferee(eq(id), argThat(v -> v.name().equals(contentDto.name())));

            mvc.perform(
                            put("/api/referees/" + id)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(bodyJson)
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().string(expectedJson));
        }
    }

    @Test
    @DisplayName("GET /api/referees?name= returns 400 when `name` is not provided")
    public void getRefereesByName_NameNotProvided_StatusBadRequest() throws Exception {
        mvc.perform(
                        get("/api/referees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]", is("query parameter 'name' not provided")));
    }

    @Test
    @DisplayName("GET /api/referees?name returns 200 when `name` is provided and pageable is default")
    public void getRefereesByName_NameProvidedWithDefaultPageable_StatusOk() throws Exception {
        var pValue = "test";
        var defaultPageNumber = 0;
        var defaultPageSize = 20;
        var expectedPageable = Pageable.ofSize(defaultPageSize).withPage(defaultPageNumber);

        Page<RefereeDto> expectedPage = Page.empty(expectedPageable);

        //given
        given(refereeService.findRefereesByName(
                eq(pValue),
                argThat(p -> p.getPageSize() == defaultPageSize && p.getPageNumber() == defaultPageNumber)
        )).willReturn(expectedPage);

        mvc.perform(
                        get("/api/referees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("name", pValue)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(0)));
    }

    @Test
    @DisplayName("GET /api/referees?name returns 200 when `name` is provided and pageable values are custom")
    public void getRefereesByName_NameProvidedWithCustomPageParameters_StatusOk() throws Exception {
        var pValue = "test";
        var testPageSize = 7;
        var testPageNumber = 4;
        var expectedPageable = Pageable.ofSize(testPageSize).withPage(testPageNumber);
        var expectedContent = List.of(RefereeDto.from(UUID.randomUUID(), "test"));
        Page<RefereeDto> expectedPage = new PageImpl<>(expectedContent, expectedPageable, 1);

        //given
        given(refereeService.findRefereesByName(
                eq(pValue),
                argThat(p -> p.getPageNumber() == testPageNumber && p.getPageSize() == testPageSize)
        )).willReturn(expectedPage);

        mvc.perform(
                        get("/api/referees")
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
