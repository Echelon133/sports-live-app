package ml.echelon133.matchservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class TestControllerTests {

    private MockMvc mvc;

    @InjectMocks
    private TestController testController;

    @BeforeEach
    public void beforeEach() {
        mvc = MockMvcBuilders.standaloneSetup(testController).build();
    }


    @Test
    @DisplayName("GET testMessage returns expected value")
    public void testMessage_ValidRequest_ReturnsExpectedValue() throws Exception {
        mvc.perform(
                get("/api/test")
                        .accept(MediaType.APPLICATION_JSON)

        )
                .andExpect(status().isOk())
                .andExpect(content().string("Test"));
    }
}