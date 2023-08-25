package ml.echelon133.matchservice.country.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import ml.echelon133.common.country.dto.CountryDto;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.matchservice.country.model.Country;
import ml.echelon133.matchservice.country.model.UpsertCountryDto;
import ml.echelon133.matchservice.country.service.CountryService;
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
public class CountryControllerTests {

    private MockMvc mvc;

    @Mock
    private CountryService countryService;

    @InjectMocks
    private CountryExceptionHandler countryExceptionHandler;

    @InjectMocks
    private CountryController countryController;

    private JacksonTester<CountryDto> jsonCountryDto;

    private JacksonTester<UpsertCountryDto> jsonUpsertCountryDto;

    @BeforeEach
    public void beforeEach() {
        JacksonTester.initFields(this, new ObjectMapper());
        mvc = MockMvcBuilders.standaloneSetup(countryController)
                .setControllerAdvice(countryExceptionHandler)
                .setCustomArgumentResolvers(
                        // required while testing controller methods which use Pageable
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }


    @Test
    @DisplayName("GET /api/countries/:id returns 404 when resource not found")
    public void getCountryById_CountryNotFound_StatusNotFound() throws Exception {
        var testId = UUID.randomUUID();

        // given
        given(countryService.findById(testId)).willThrow(
                new ResourceNotFoundException(Country.class, testId)
        );

        mvc.perform(
                        get("/api/countries/" + testId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("country %s could not be found", testId)
                )));
    }

    @Test
    @DisplayName("GET /api/countries/:id returns 200 and a valid entity if entity found")
    public void getCountryById_CountryFound_StatusOk() throws Exception {
        var testId = UUID.randomUUID();
        var countryDto = CountryDto.from(testId, "Test", "PL");

        var expectedJson = jsonCountryDto.write(countryDto).getJson();

        // given
        given(countryService.findById(testId)).willReturn(countryDto);

        mvc.perform(
                        get("/api/countries/" + testId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("POST /api/countries returns 422 when name is not provided")
    public void createCountry_NameNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = new UpsertCountryDto(null, "DE");
        var json = jsonUpsertCountryDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/countries")
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
    @DisplayName("POST /api/countries returns 422 when name length is incorrect")
    public void createCountry_NameLengthIncorrect_StatusUnprocessableEntity() throws Exception {
        var incorrectNameLengths = List.of(
                // too short (0 characters)
                "",
                /// too long (101 characters)
                "6EUxnEDmbw2qPIeUhhjnVrLJbnjdslg8dp1HUac4oK7tzkIo9QkQ125QTTT6oPmzk7pfo28BrGINDzbXBYZdKGTMhSEtOGf1g6wgb"
        );

        for (String incorrectName : incorrectNameLengths) {
            var contentDto = new UpsertCountryDto(incorrectName, "PL");
            var json = jsonUpsertCountryDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/countries")
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
    @DisplayName("POST /api/countries returns 200 when name length is correct")
    public void createCountry_NameLengthIsCorrect_StatusOk() throws Exception {
        var correctNameLengths = List.of(
                // 1 character (lower limit)
                "a",
                // 100 characters (upper limit)
                "6EUxnEDmbw2qPIeUhhjnVrLJbnjdslg8dp1HUac4oK7tzkIo9QkQ125QTTT6oPmzk7pfo28BrGINDzbXBYZdKGTMhSEtOGf1g6wg"
        );

        for (String correctName : correctNameLengths) {
            // expected from the database
            var dto = CountryDto.from(UUID.randomUUID(), correctName, "PL");
            var expectedJson = jsonCountryDto.write(dto).getJson();

            // what is given in the request body
            var contentDto = new UpsertCountryDto(correctName, "PL");
            var bodyJson = jsonUpsertCountryDto.write(contentDto).getJson();

            // use doReturn, because regular given/when does not work when re-declaring a single argThat matcher
            doReturn(dto).when(countryService).createCountry(argThat(v -> v.getName().equals(contentDto.getName())));

            mvc.perform(
                            post("/api/countries")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(bodyJson)
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().string(expectedJson));
        }
    }

    @Test
    @DisplayName("POST /api/countries returns 422 when country code is not provided")
    public void createCountry_CountryCodeNotProvided_StatusUnprocessableEntity() throws Exception {
        var contentDto = new UpsertCountryDto("Poland", null);
        var json = jsonUpsertCountryDto.write(contentDto).getJson();

        mvc.perform(
                        post("/api/countries")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("countryCode", List.of("invalid ISO 3166-1 alpha-2 country code")))
                );
    }

    @Test
    @DisplayName("POST /api/countries returns 422 when country code is incorrect")
    public void createCountry_CountryCodeIncorrect_StatusUnprocessableEntity() throws Exception {
        var incorrectCountryCodes = List.of(
                "A", "a", "A1", "1A", "asdf", "ASDF", "10", "01"
        );

        for (String incorrectCountryCode : incorrectCountryCodes) {
            var contentDto = new UpsertCountryDto("Poland", incorrectCountryCode);
            var json = jsonUpsertCountryDto.write(contentDto).getJson();

            mvc.perform(
                            post("/api/countries")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("countryCode", List.of("invalid ISO 3166-1 alpha-2 country code")))
                    );
        }
    }

    @Test
    @DisplayName("POST /api/countries returns 200 when uppercase country code is correct")
    public void createCountry_UppercaseCountryCodeIsCorrect_StatusOk() throws Exception {
        for (int firstChar = 'A'; firstChar <= 'Z'; firstChar++) {
            for (int secondChar = 'A'; secondChar <= 'Z'; secondChar++) {
                var testCountryCode = String.format("%c%c", firstChar, secondChar);

                var dto = CountryDto.from(UUID.randomUUID(), "Test", testCountryCode);
                var expectedJson = jsonCountryDto.write(dto).getJson();

                // what is given in the request body
                var contentDto = new UpsertCountryDto("Poland", testCountryCode);
                var bodyJson = jsonUpsertCountryDto.write(contentDto).getJson();

                // use doReturn, because regular given/when does not work when re-declaring a single argThat matcher
                doReturn(dto).when(countryService).createCountry(argThat(v -> v.getName().equals(contentDto.getName())));

                mvc.perform(
                                post("/api/countries")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .accept(MediaType.APPLICATION_JSON)
                                        .content(bodyJson)
                        )
                        .andExpect(status().isOk())
                        .andExpect(content().string(expectedJson));
            }
        }
    }

    @Test
    @DisplayName("POST /api/countries returns 200 when lowercase country code is correct")
    public void createCountry_LowercaseCountryCodeIsCorrect_StatusOk() throws Exception {

        for (int firstChar = 'a'; firstChar <= 'z'; firstChar++) {
            for (int secondChar = 'a'; secondChar <= 'z'; secondChar++) {
                var testCountryCode = String.format("%c%c", firstChar, secondChar);

                var dto = CountryDto.from(UUID.randomUUID(), "Test", testCountryCode);
                var expectedJson = jsonCountryDto.write(dto).getJson();

                // what is given in the request body
                var contentDto = new UpsertCountryDto("Poland", testCountryCode);
                var bodyJson = jsonUpsertCountryDto.write(contentDto).getJson();

                // use doReturn, because regular given/when does not work when re-declaring a single argThat matcher
                doReturn(dto).when(countryService).createCountry(argThat(v -> v.getName().equals(contentDto.getName())));

                mvc.perform(
                                post("/api/countries")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .accept(MediaType.APPLICATION_JSON)
                                        .content(bodyJson)
                        )
                        .andExpect(status().isOk())
                        .andExpect(content().string(expectedJson));
            }
        }
    }

    @Test
    @DisplayName("DELETE /api/countries/:id returns 200 and a counter of how many countries have been deleted")
    public void deleteCountry_CountryIdProvided_StatusOkAndReturnsCounter() throws Exception {
        var id = UUID.randomUUID();

        // given
        given(countryService.markCountryAsDeleted(id)).willReturn(1);

        mvc.perform(
                        delete("/api/countries/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("{\"deleted\":1}"));
    }

    @Test
    @DisplayName("PUT /api/countries/:id returns 404 when resource not found")
    public void updateCountry_CountryNotFound_StatusNotFound() throws Exception {
        var testId = UUID.randomUUID();
        var upsertDto = new UpsertCountryDto("Test", "PT");
        var upsertJson = jsonUpsertCountryDto.write(upsertDto).getJson();

        // given
        given(countryService.updateCountry(eq(testId), ArgumentMatchers.any())).willThrow(
                new ResourceNotFoundException(Country.class, testId)
        );

        mvc.perform(
                        put("/api/countries/" + testId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(upsertJson)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]", is(
                        String.format("country %s could not be found", testId)
                )));
    }

    @Test
    @DisplayName("PUT /api/countries/:id returns 422 when name is not provided")
    public void updateCountry_CountryFoundAndNameNotProvided_StatusUnprocessableEntity() throws Exception {
        var testId = UUID.randomUUID();
        var contentDto = new UpsertCountryDto(null, "PL");
        var json = jsonUpsertCountryDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/countries/" + testId)
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
    @DisplayName("PUT /api/countries/:id returns 422 when name length is incorrect")
    public void updateCountry_CountryFoundAndNameLengthIncorrect_StatusUnprocessableEntity() throws Exception {
        var testId = UUID.randomUUID();
        var incorrectNameLengths = List.of(
                // too short (0 characters)
                "",
                /// too long (101 characters)
                "6EUxnEDmbw2qPIeUhhjnVrLJbnjdslg8dp1HUac4oK7tzkIo9QkQ125QTTT6oPmzk7pfo28BrGINDzbXBYZdKGTMhSEtOGf1g6wgb"
        );

        for (String incorrectName : incorrectNameLengths) {
            var contentDto = new UpsertCountryDto(incorrectName, "PL");
            var json = jsonUpsertCountryDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/countries/" + testId)
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
    @DisplayName("PUT /api/countries/:id returns 200 when name length is correct")
    public void updateCountry_CountryFoundAndNameLengthIsCorrect_StatusOk() throws Exception {
        var id = UUID.randomUUID();

        var correctNameLengths = List.of(
                // 1 character (lower limit)
                "a",
                // 100 characters (upper limit)
                "6EUxnEDmbw2qPIeUhhjnVrLJbnjdslg8dp1HUac4oK7tzkIo9QkQ125QTTT6oPmzk7pfo28BrGINDzbXBYZdKGTMhSEtOGf1g6wg"
        );

        for (String correctName : correctNameLengths) {
            // expected from the database
            var dto = CountryDto.from(UUID.randomUUID(), correctName, "PL");
            var expectedJson = jsonCountryDto.write(dto).getJson();

            // what is given in the request body
            var contentDto = new UpsertCountryDto(correctName, "PL");
            var bodyJson = jsonUpsertCountryDto.write(contentDto).getJson();

            // use doReturn, because regular given/when does not work when re-declaring a single argThat matcher
            doReturn(dto).when(countryService).updateCountry(eq(id), argThat(v -> v.getName().equals(contentDto.getName())));

            mvc.perform(
                            put("/api/countries/" + id)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(bodyJson)
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().string(expectedJson));
        }
    }

    // TODO: put for country code
    @Test
    @DisplayName("PUT /api/countries/:id returns 422 when country code is not provided")
    public void updateCountry_CountryFoundAndCountryCodeNotProvided_StatusUnprocessableEntity() throws Exception {
        var testId = UUID.randomUUID();
        var contentDto = new UpsertCountryDto("Poland", null);
        var json = jsonUpsertCountryDto.write(contentDto).getJson();

        mvc.perform(
                        put("/api/countries/" + testId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.messages", hasEntry("countryCode", List.of("invalid ISO 3166-1 alpha-2 country code")))
                );
    }

    @Test
    @DisplayName("PUT /api/countries/:id returns 422 when name length is incorrect")
    public void updateCountry_CountryFoundAndCountryCodeIncorrect_StatusUnprocessableEntity() throws Exception {
        var testId = UUID.randomUUID();

        var incorrectCountryCodes = List.of(
                "A", "a", "A1", "1A", "asdf", "ASDF", "10", "01"
        );

        for (String incorrectCountryCode : incorrectCountryCodes) {
            var contentDto = new UpsertCountryDto("Poland", incorrectCountryCode);
            var json = jsonUpsertCountryDto.write(contentDto).getJson();

            mvc.perform(
                            put("/api/countries/" + testId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(
                            jsonPath("$.messages", hasEntry("countryCode", List.of("invalid ISO 3166-1 alpha-2 country code")))
                    );
        }
    }

    @Test
    @DisplayName("PUT /api/countries/:id returns 200 when uppercase country code is correct")
    public void updateCountry_CountryFoundAndUppercaseCountryCodeIsCorrect_StatusOk() throws Exception {
        var id = UUID.randomUUID();

        for (int firstChar = 'A'; firstChar <= 'Z'; firstChar++) {
            for (int secondChar = 'A'; secondChar <= 'Z'; secondChar++) {
                var testCountryCode = String.format("%c%c", firstChar, secondChar);

                var dto = CountryDto.from(UUID.randomUUID(), "Test", testCountryCode);
                var expectedJson = jsonCountryDto.write(dto).getJson();

                // what is given in the request body
                var contentDto = new UpsertCountryDto("Poland", testCountryCode);
                var bodyJson = jsonUpsertCountryDto.write(contentDto).getJson();

                // use doReturn, because regular given/when does not work when re-declaring a single argThat matcher
                doReturn(dto).when(countryService).updateCountry(
                        eq(id),
                        argThat(v -> v.getName().equals(contentDto.getName()) && v.getCountryCode().equals(contentDto.getCountryCode())
                ));

                mvc.perform(
                                put("/api/countries/" + id)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .accept(MediaType.APPLICATION_JSON)
                                        .content(bodyJson)
                        )
                        .andExpect(status().isOk())
                        .andExpect(content().string(expectedJson));
            }
        }
    }

    @Test
    @DisplayName("PUT /api/countries/:id returns 200 when lowercase country code is correct")
    public void updateCountry_CountryFoundAndLowercaseCountryCodeIsCorrect_StatusOk() throws Exception {
        var id = UUID.randomUUID();

        for (int firstChar = 'a'; firstChar <= 'z'; firstChar++) {
            for (int secondChar = 'a'; secondChar <= 'z'; secondChar++) {
                var testCountryCode = String.format("%c%c", firstChar, secondChar);

                var dto = CountryDto.from(UUID.randomUUID(), "Test", testCountryCode);
                var expectedJson = jsonCountryDto.write(dto).getJson();

                // what is given in the request body
                var contentDto = new UpsertCountryDto("Poland", testCountryCode);
                var bodyJson = jsonUpsertCountryDto.write(contentDto).getJson();

                // use doReturn, because regular given/when does not work when re-declaring a single argThat matcher
                doReturn(dto).when(countryService).updateCountry(
                        eq(id),
                        argThat(v -> v.getName().equals(contentDto.getName()) && v.getCountryCode().equals(contentDto.getCountryCode())
                        ));

                mvc.perform(
                                put("/api/countries/" + id)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .accept(MediaType.APPLICATION_JSON)
                                        .content(bodyJson)
                        )
                        .andExpect(status().isOk())
                        .andExpect(content().string(expectedJson));
            }
        }
    }

    @Test
    @DisplayName("GET /api/countries?name= returns 400 when `name` is not provided")
    public void getCountriesByName_NameNotProvided_StatusBadRequest() throws Exception {
        mvc.perform(
                        get("/api/countries")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]", is("'name' request parameter is required")));
    }

    @Test
    @DisplayName("GET /api/countries?name returns 200 when `name` is provided and pageable is default")
    public void getCountriesByName_NameProvidedWithDefaultPageable_StatusOk() throws Exception {
        var pValue = "test";
        var defaultPageNumber = 0;
        var defaultPageSize = 20;

        Page<CountryDto> expectedPage = Page.empty();

        //given
        given(countryService.findCountriesByName(
                eq(pValue),
                argThat(p -> p.getPageSize() == defaultPageSize && p.getPageNumber() == defaultPageNumber)
        )).willReturn(expectedPage);

        mvc.perform(
                        get("/api/countries")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("name", pValue)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(0)));
    }

    @Test
    @DisplayName("GET /api/countries?name returns 200 when `name` is provided and pageable values are custom")
    public void getCountriesByName_NameProvidedWithCustomPageParameters_StatusOk() throws Exception {
        var pValue = "test";
        var testPageSize = 7;
        var testPageNumber = 4;
        var expectedPageable = Pageable.ofSize(testPageSize).withPage(testPageNumber);
        var expectedContent = List.of(CountryDto.from(UUID.randomUUID(), "test", "PL"));
        Page<CountryDto> expectedPage = new PageImpl<>(expectedContent, expectedPageable, 1);

        //given
        given(countryService.findCountriesByName(
                eq(pValue),
                argThat(p -> p.getPageNumber() == testPageNumber && p.getPageSize() == testPageSize)
        )).willReturn(expectedPage);

        mvc.perform(
                        get("/api/countries")
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
