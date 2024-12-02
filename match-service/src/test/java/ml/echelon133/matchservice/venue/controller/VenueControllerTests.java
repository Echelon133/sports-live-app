package ml.echelon133.matchservice.venue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.matchservice.venue.model.VenueDto;
import ml.echelon133.matchservice.venue.model.UpsertVenueDto;
import ml.echelon133.matchservice.venue.model.Venue;
import ml.echelon133.matchservice.venue.service.VenueService;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(MockitoExtension.class)
public class VenueControllerTests {

    private MockMvc mvc;

    @Mock
    private VenueService venueService;

    @InjectMocks
    private VenueExceptionHandler venueExceptionHandler;

    @InjectMocks
    private VenueController venueController;

    private JacksonTester<VenueDto> jsonVenueDto;

    private JacksonTester<UpsertVenueDto> jsonUpsertVenueDto;

    @BeforeEach
    public void beforeEach() {
        JacksonTester.initFields(this, new ObjectMapper());
        mvc = MockMvcBuilders.standaloneSetup(venueController)
                .setControllerAdvice(venueExceptionHandler)
                .setCustomArgumentResolvers(
                        // required while testing controller methods which use Pageable
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    @DisplayName("GET /api/venues/:id returns 404 when resource not found")
    public void getVenueById_VenueNotFound_StatusNotFound() throws Exception {
        var testId = UUID.randomUUID();

        // given
        given(venueService.findById(testId)).willThrow(
                new ResourceNotFoundException(Venue.class, testId)
        );

        mvc.perform(
                get("/api/venues/" + testId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("venue %s could not be found", testId)
                )));
    }

    @Test
    @DisplayName("GET /api/venues/:id returns 200 and a valid entity if entity found")
    public void getVenueById_VenueFound_StatusOk() throws Exception {
        var testId = UUID.randomUUID();
        var venueDto = VenueDto.from(testId, "San Siro", 80018);

        var expectedJson = jsonVenueDto.write(venueDto).getJson();

        // given
        given(venueService.findById(testId)).willReturn(venueDto);

        mvc.perform(
                get("/api/venues/" + testId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("POST /api/venues returns 422 when name is not provided")
    public void createVenue_NameNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = new UpsertVenueDto(null, 100);
        var json = jsonUpsertVenueDto.write(contentDto).getJson();

        mvc.perform(
                post("/api/venues")
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
    @DisplayName("POST /api/venues returns 422 when name length is incorrect")
    public void createVenue_NameLengthIncorrect_StatusUnprocessableEntity() throws Exception {
        var incorrectNameLengths = List.of(
                // too short (0 characters)
                "",
                /// too long (121 characters)
                "6EUxnEDmbw2qPIeUhhjnVrLJbnjdslg8dp1HUac4oK7tzkIo9QkQ125QTTT6oPmzk7pfo28BrGINDzbXBYZdKGTMhSEtOGf1g6wgbmJccybyeR3qFqpsEq2eC"
        );

        for (String incorrectName : incorrectNameLengths) {
            var contentDto = new UpsertVenueDto(incorrectName, 100);
            var json = jsonUpsertVenueDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/venues")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("name", List.of("expected length between 1 and 120")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/venues returns 422 when capacity is negative")
    public void createVenue_CapacityNotPositive_StatusUnprocessableEntity() throws Exception {
        var incorrectCapacities = List.of(-100, -1);

        for (Integer incorrectCapacity: incorrectCapacities) {
            var contentDto = new UpsertVenueDto("a", incorrectCapacity);
            var json = jsonUpsertVenueDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/venues")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("capacity", List.of("expected positive integers")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/venues returns 200 when name length is correct and capacity is positive")
    public void createVenue_NameLengthIsCorrectAndCapacityPositive_StatusOk() throws Exception {
        var correctNameLengths = List.of(
                // 1 character (lower limit)
                "a",
                // 120 characters (upper limit)
                "6EUxnEDmbw2qPIeUhhjnVrLJbnjdslg8dp1HUac4oK7tzkIo9QkQ125QTTT6oPmzk7pfo28BrGINDzbXBYZdKGTMhSEtOGf1g6wgbmJccybyeR3qFqpsEq2e"
        );

        for (String correctName : correctNameLengths) {
            // expected from the database
            var dto = VenueDto.from(UUID.randomUUID(), correctName, 0);
            var expectedJson = jsonVenueDto.write(dto).getJson();

            // what is given in the request body
            var contentDto = new UpsertVenueDto(correctName, 0);
            var bodyJson = jsonUpsertVenueDto.write(contentDto).getJson();

            // use doReturn, because regular given/when does not work when re-declaring a single argThat matcher
            doReturn(dto).when(venueService).createVenue(
                    argThat(v -> v.name().equals(contentDto.name()) && v.capacity().intValue() == contentDto.capacity().intValue())
            );

            mvc.perform(
                            post("/api/venues")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(bodyJson)
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().string(expectedJson));
        }
    }

    @Test
    @DisplayName("DELETE /api/venues/:id returns 200 and a counter of how many venues have been deleted")
    public void deleteVenue_VenueIdProvided_StatusOkAndReturnsCounter() throws Exception {
        var id = UUID.randomUUID();

        // given
        given(venueService.markVenueAsDeleted(id)).willReturn(1);

        mvc.perform(
                        delete("/api/venues/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("{\"deleted\":1}"));
    }

    @Test
    @DisplayName("PUT /api/venues/:id returns 404 when resource not found")
    public void updateVenue_VenueNotFound_StatusNotFound() throws Exception {
        var testId = UUID.randomUUID();
        var upsertDto = new UpsertVenueDto("Test", null);
        var upsertJson = jsonUpsertVenueDto.write(upsertDto).getJson();

        // given
        given(venueService.updateVenue(eq(testId), ArgumentMatchers.any())).willThrow(
                new ResourceNotFoundException(Venue.class, testId)
        );

        mvc.perform(
                        put("/api/venues/" + testId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(upsertJson)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("venue %s could not be found", testId)
                )));
    }

    @Test
    @DisplayName("PUT /api/venues/:id returns 422 when name is not provided")
    public void updateVenue_VenueFoundAndNameNotProvided_StatusUnprocessableEntity() throws Exception {
        var testId = UUID.randomUUID();
        var contentDto = new UpsertVenueDto(null, 100);
        var json = jsonUpsertVenueDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/venues/" + testId)
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
    @DisplayName("PUT /api/venues/:id returns 422 when name length is incorrect")
    public void updateVenue_VenueFoundAndNameLengthIncorrect_StatusUnprocessableEntity() throws Exception {
        var testId = UUID.randomUUID();
        var incorrectNameLengths = List.of(
                // too short (0 characters)
                "",
                /// too long (121 characters)
                "6EUxnEDmbw2qPIeUhhjnVrLJbnjdslg8dp1HUac4oK7tzkIo9QkQ125QTTT6oPmzk7pfo28BrGINDzbXBYZdKGTMhSEtOGf1g6wgbmJccybyeR3qFqpsEq2eC"
        );

        for (String incorrectName : incorrectNameLengths) {
            var contentDto = new UpsertVenueDto(incorrectName, 100);
            var json = jsonUpsertVenueDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/venues/" + testId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("name", List.of("expected length between 1 and 120")))
                    );
        }
    }

    @Test
    @DisplayName("PUT /api/venues/:id returns 422 when capacity is negative")
    public void updateVenue_VenueFoundAndCapacityNotPositive_StatusUnprocessableEntity() throws Exception {
        var testId = UUID.randomUUID();
        var incorrectCapacities = List.of(-100, -1);

        for (Integer incorrectCapacity : incorrectCapacities) {
            var contentDto = new UpsertVenueDto("a", incorrectCapacity);
            var json = jsonUpsertVenueDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/venues/" + testId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("capacity", List.of("expected positive integers")))
                    );
        }
    }

    @Test
    @DisplayName("PUT /api/venues/:id returns 200 when name length is correct and capacity is positive")
    public void updateVenue_VenueFoundAndNameLengthIsCorrectAndCapacityPositive_StatusOk() throws Exception {
        var id = UUID.randomUUID();

        var correctNameLengths = List.of(
                // 1 character (lower limit)
                "a",
                // 120 characters (upper limit)
                "6EUxnEDmbw2qPIeUhhjnVrLJbnjdslg8dp1HUac4oK7tzkIo9QkQ125QTTT6oPmzk7pfo28BrGINDzbXBYZdKGTMhSEtOGf1g6wgbmJccybyeR3qFqpsEq2e"
        );

        for (String correctName : correctNameLengths) {
            // expected from the database
            var dto = VenueDto.from(UUID.randomUUID(), correctName, 0);
            var expectedJson = jsonVenueDto.write(dto).getJson();

            // what is given in the request body
            var contentDto = new UpsertVenueDto(correctName, 0);
            var bodyJson = jsonUpsertVenueDto.write(contentDto).getJson();

            // use doReturn, because regular given/when does not work when re-declaring a single argThat matcher
            doReturn(dto).when(venueService).updateVenue(
                    eq(id),
                    argThat(v -> v.name().equals(contentDto.name()) && v.capacity().intValue() == contentDto.capacity().intValue())
            );

            mvc.perform(
                            put("/api/venues/" + id)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(bodyJson)
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().string(expectedJson));
        }
    }

    @Test
    @DisplayName("GET /api/venues?name= returns 400 when `name` is not provided")
    public void getVenuesByName_NameNotProvided_StatusBadRequest() throws Exception {
        mvc.perform(
                        get("/api/venues")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]", is("query parameter 'name' not provided")));
    }

    @Test
    @DisplayName("GET /api/venues?name returns 200 when `name` is provided and pageable is default")
    public void getVenuesByName_NameProvidedWithDefaultPageable_StatusOk() throws Exception {
        var pValue = "test";
        var defaultPageNumber = 0;
        var defaultPageSize = 20;
        var expectedPageable = Pageable.ofSize(defaultPageSize).withPage(defaultPageNumber);

        Page<VenueDto> expectedPage = Page.empty(expectedPageable);

        //given
        given(venueService.findVenuesByName(
                eq(pValue),
                argThat(p -> p.getPageSize() == defaultPageSize && p.getPageNumber() == defaultPageNumber)
        )).willReturn(expectedPage);

        mvc.perform(
                        get("/api/venues")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("name", pValue)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(0)));
    }

    @Test
    @DisplayName("GET /api/venues?name returns 200 when `name` is provided and pageable values are custom")
    public void getVenuesByName_NameProvidedWithCustomPageParameters_StatusOk() throws Exception {
        var pValue = "test";
        var testPageSize = 7;
        var testPageNumber = 4;
        var expectedPageable = Pageable.ofSize(testPageSize).withPage(testPageNumber);
        var expectedContent = List.of(VenueDto.from(UUID.randomUUID(), "test", null));
        Page<VenueDto> expectedPage = new PageImpl<>(expectedContent, expectedPageable, 1);

        //given
        given(venueService.findVenuesByName(
                eq(pValue),
                argThat(p -> p.getPageNumber() == testPageNumber && p.getPageSize() == testPageSize)
        )).willReturn(expectedPage);

        mvc.perform(
                        get("/api/venues")
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
