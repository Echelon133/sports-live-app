package ml.echelon133.matchservice.venue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.venue.dto.VenueDto;
import ml.echelon133.matchservice.venue.model.Venue;
import ml.echelon133.matchservice.venue.service.VenueService;
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

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
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

    @BeforeEach
    public void beforeEach() {
        JacksonTester.initFields(this, new ObjectMapper());
        mvc = MockMvcBuilders.standaloneSetup(venueController).setControllerAdvice(venueExceptionHandler).build();
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
        var venueDto = new VenueDto(testId, "San Siro", 80018);

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
}
