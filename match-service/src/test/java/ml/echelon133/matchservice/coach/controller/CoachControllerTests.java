package ml.echelon133.matchservice.coach.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.matchservice.coach.model.Coach;
import ml.echelon133.matchservice.coach.model.CoachDto;
import ml.echelon133.matchservice.coach.model.UpsertCoachDto;
import ml.echelon133.matchservice.coach.service.CoachService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class CoachControllerTests {

    private MockMvc mvc;

    @Mock
    private CoachService coachService;

    @InjectMocks
    private CoachExceptionHandler coachExceptionHandler;

    @InjectMocks
    private CoachController coachController;

    private JacksonTester<CoachDto> jsonCoachDto;

    private JacksonTester<UpsertCoachDto> jsonUpsertCoachDto;

    @BeforeEach
    public void beforeEach() {
        JacksonTester.initFields(this, new ObjectMapper());
        mvc = MockMvcBuilders.standaloneSetup(coachController)
                .setControllerAdvice(coachExceptionHandler)
                .setCustomArgumentResolvers(
                        // required while testing controller methods which use Pageable
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    @DisplayName("GET /api/coaches/:id returns 404 when resource not found")
    public void getCoachById_CoachNotFound_StatusNotFound() throws Exception {
        var testId = UUID.randomUUID();

        // given
        given(coachService.findById(testId)).willThrow(
                new ResourceNotFoundException(Coach.class, testId)
        );

        mvc.perform(
                        get("/api/coaches/" + testId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("coach %s could not be found", testId)
                )));
    }

    @Test
    @DisplayName("GET /api/coaches/:id returns 200 and a valid entity if entity found")
    public void getCoachById_CoachFound_StatusOk() throws Exception {
        var testId = UUID.randomUUID();
        var coachDto = CoachDto.from(testId, "Test");

        var expectedJson = jsonCoachDto.write(coachDto).getJson();

        // given
        given(coachService.findById(testId)).willReturn(coachDto);

        mvc.perform(
                        get("/api/coaches/" + testId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("POST /api/coaches returns 422 when name is not provided")
    public void createCoach_NameNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = new UpsertCoachDto(null);
        var json = jsonUpsertCoachDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/coaches")
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
    @DisplayName("POST /api/coaches returns 422 when name length is incorrect")
    public void createCoach_NameLengthIncorrect_StatusUnprocessableEntity() throws Exception {
        var incorrectNameLengths = List.of(
                // too short (0 characters)
                "",
                /// too long (101 characters)
                "6EUxnEDmbw2qPIeUhhjnVrLJbnjdslg8dp1HUac4oK7tzkIo9QkQ125QTTT6oPmzk7pfo28BrGINDzbXBYZdKGTMhSEtOGf1g6wgb"
        );

        for (String incorrectName : incorrectNameLengths) {
            var contentDto = new UpsertCoachDto(incorrectName);
            var json = jsonUpsertCoachDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/coaches")
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
    @DisplayName("POST /api/coaches returns 200 when name length is correct")
    public void createCoach_NameLengthIsCorrect_StatusOk() throws Exception {
        var correctNameLengths = List.of(
                // 1 character (lower limit)
                "a",
                // 100 characters (upper limit)
                "6EUxnEDmbw2qPIeUhhjnVrLJbnjdslg8dp1HUac4oK7tzkIo9QkQ125QTTT6oPmzk7pfo28BrGINDzbXBYZdKGTMhSEtOGf1g6wg"
        );

        for (String correctName : correctNameLengths) {
            // expected from the database
            var dto = CoachDto.from(UUID.randomUUID(), correctName);
            var expectedJson = jsonCoachDto.write(dto).getJson();

            // what is given in the request body
            var contentDto = new UpsertCoachDto(correctName);
            var bodyJson = jsonUpsertCoachDto.write(contentDto).getJson();

            // use doReturn, because regular given/when does not work when re-declaring a single argThat matcher
            doReturn(dto).when(coachService).createCoach(argThat(v -> v.getName().equals(contentDto.getName())));

            mvc.perform(
                            post("/api/coaches")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(bodyJson)
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().string(expectedJson));
        }
    }

    @Test
    @DisplayName("DELETE /api/coaches/:id returns 200 and a counter of how many coaches have been deleted")
    public void deleteCoach_CoachIdProvided_StatusOkAndReturnsCounter() throws Exception {
        var id = UUID.randomUUID();

        // given
        given(coachService.markCoachAsDeleted(id)).willReturn(1);

        mvc.perform(
                        delete("/api/coaches/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("{\"deleted\":1}"));
    }

    @Test
    @DisplayName("PUT /api/coaches/:id returns 404 when resource not found")
    public void updateCoach_CoachNotFound_StatusNotFound() throws Exception {
        var testId = UUID.randomUUID();
        var upsertDto = new UpsertCoachDto("Test");
        var upsertJson = jsonUpsertCoachDto.write(upsertDto).getJson();

        // given
        given(coachService.updateCoach(eq(testId), ArgumentMatchers.any())).willThrow(
                new ResourceNotFoundException(Coach.class, testId)
        );

        mvc.perform(
                        put("/api/coaches/" + testId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(upsertJson)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("coach %s could not be found", testId)
                )));
    }

    @Test
    @DisplayName("PUT /api/coaches/:id returns 422 when name is not provided")
    public void updateCoach_CoachFoundAndNameNotProvided_StatusUnprocessableEntity() throws Exception {
        var testId = UUID.randomUUID();
        var contentDto = new UpsertCoachDto(null);
        var json = jsonUpsertCoachDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/coaches/" + testId)
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
    @DisplayName("PUT /api/coaches/:id returns 422 when name length is incorrect")
    public void updateCoach_CoachFoundAndNameLengthIncorrect_StatusUnprocessableEntity() throws Exception {
        var testId = UUID.randomUUID();
        var incorrectNameLengths = List.of(
                // too short (0 characters)
                "",
                /// too long (101 characters)
                "6EUxnEDmbw2qPIeUhhjnVrLJbnjdslg8dp1HUac4oK7tzkIo9QkQ125QTTT6oPmzk7pfo28BrGINDzbXBYZdKGTMhSEtOGf1g6wgb"
        );

        for (String incorrectName : incorrectNameLengths) {
            var contentDto = new UpsertCoachDto(incorrectName);
            var json = jsonUpsertCoachDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/coaches/" + testId)
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
    @DisplayName("PUT /api/coaches/:id returns 200 when name length is correct")
    public void updateCoach_CoachFoundAndNameLengthIsCorrect_StatusOk() throws Exception {
        var id = UUID.randomUUID();

        var correctNameLengths = List.of(
                // 1 character (lower limit)
                "a",
                // 100 characters (upper limit)
                "6EUxnEDmbw2qPIeUhhjnVrLJbnjdslg8dp1HUac4oK7tzkIo9QkQ125QTTT6oPmzk7pfo28BrGINDzbXBYZdKGTMhSEtOGf1g6wg"
        );

        for (String correctName : correctNameLengths) {
            // expected from the database
            var dto = CoachDto.from(UUID.randomUUID(), correctName);
            var expectedJson = jsonCoachDto.write(dto).getJson();

            // what is given in the request body
            var contentDto = new UpsertCoachDto(correctName);
            var bodyJson = jsonUpsertCoachDto.write(contentDto).getJson();

            // use doReturn, because regular given/when does not work when re-declaring a single argThat matcher
            doReturn(dto).when(coachService).updateCoach(eq(id), argThat(v -> v.getName().equals(contentDto.getName())));

            mvc.perform(
                            put("/api/coaches/" + id)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(bodyJson)
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().string(expectedJson));
        }
    }

    @Test
    @DisplayName("GET /api/coaches?name= returns 400 when `name` is not provided")
    public void getCoachesByName_NameNotProvided_StatusBadRequest() throws Exception {
        mvc.perform(
                        get("/api/coaches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]", is("query parameter 'name' not provided")));
    }

    @Test
    @DisplayName("GET /api/coaches?name returns 200 when `name` is provided and pageable is default")
    public void getCoachesByName_NameProvidedWithDefaultPageable_StatusOk() throws Exception {
        var pValue = "test";
        var defaultPageNumber = 0;
        var defaultPageSize = 20;
        var expectedPageable = Pageable.ofSize(defaultPageSize).withPage(defaultPageNumber);

        Page<CoachDto> expectedPage = Page.empty(expectedPageable);

        //given
        given(coachService.findCoachesByName(
                eq(pValue),
                argThat(p -> p.getPageSize() == defaultPageSize && p.getPageNumber() == defaultPageNumber)
        )).willReturn(expectedPage);

        mvc.perform(
                        get("/api/coaches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("name", pValue)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(0)));
    }

    @Test
    @DisplayName("GET /api/coaches?name returns 200 when `name` is provided and pageable values are custom")
    public void getCoachesByName_NameProvidedWithCustomPageParameters_StatusOk() throws Exception {
        var pValue = "test";
        var testPageSize = 7;
        var testPageNumber = 4;
        var expectedPageable = Pageable.ofSize(testPageSize).withPage(testPageNumber);
        var expectedContent = List.of(CoachDto.from(UUID.randomUUID(), "test"));
        Page<CoachDto> expectedPage = new PageImpl<>(expectedContent, expectedPageable, 1);

        //given
        given(coachService.findCoachesByName(
                eq(pValue),
                argThat(p -> p.getPageNumber() == testPageNumber && p.getPageSize() == testPageSize)
        )).willReturn(expectedPage);

        mvc.perform(
                        get("/api/coaches")
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
