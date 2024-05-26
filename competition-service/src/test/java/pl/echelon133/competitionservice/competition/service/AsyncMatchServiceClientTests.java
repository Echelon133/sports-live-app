package pl.echelon133.competitionservice.competition.service;


import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.exceptions.misusing.InvalidUseOfMatchersException;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import pl.echelon133.competitionservice.competition.client.MatchServiceClient;
import pl.echelon133.competitionservice.competition.model.TeamDetailsDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class AsyncMatchServiceClientTests {

    @Mock
    private MatchServiceClient matchServiceClient;

    @Spy
    private Executor executor = Executors.newFixedThreadPool(2);

    @InjectMocks
    private AsyncMatchServiceClient asyncMatchServiceClient;


    @Test
    @DisplayName("getAllTeams throws CompletionException when the response to one of the requests is 404 NOT FOUND")
    public void getAllTeams_OneTeamNotFound_ThrowsCompletionException() {
        // 10 ids which we expect to succeed
        var okTeamIds = createTestIds(10);
        // 1 id which we expect to fail with 404 NOT_FOUND
        var notFoundTeamIds = createTestIds(1);

        List<UUID> allTeamIds = Stream
                .concat(okTeamIds.stream(), notFoundTeamIds.stream())
                .collect(Collectors.toList());

        // given
        givenTeamIdsSimulateClientResponses(okTeamIds, notFoundTeamIds, HttpStatus.NOT_FOUND);

        // when
        String message = assertThrows(CompletionException.class, () -> {
            asyncMatchServiceClient.getAllTeams(allTeamIds);
        }).getMessage();

        // then
        var expectedMessage = String.format("failed to fetch resource with id %s", notFoundTeamIds.get(0));
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("getAllTeams throws CompletionException when the response to one of the requests is 500 INTERNAL SERVER ERROR")
    public void getAllTeams_OneTeamCausesInternalServerError_ThrowsCompletionException() {
        // 10 ids which we expect to succeed
        var okTeamIds = createTestIds(10);
        // 1 id which we expect to fail with 500 INTERNAL SERVER ERROR
        var iseTeamIds = createTestIds(1);

        List<UUID> allTeamIds = Stream
                .concat(okTeamIds.stream(), iseTeamIds.stream())
                .collect(Collectors.toList());

        // given
        givenTeamIdsSimulateClientResponses(okTeamIds, iseTeamIds, HttpStatus.INTERNAL_SERVER_ERROR);

        // when
        String message = assertThrows(CompletionException.class, () -> {
            asyncMatchServiceClient.getAllTeams(allTeamIds);
        }).getMessage();

        // then
        var expectedMessage = String.format("failed to fetch resource with id %s", iseTeamIds.get(0));
        assertEquals(expectedMessage, message);
    }

    @Test
    @DisplayName("getAllTeams returns a (id -> team) map if not a single request fails")
    public void getAllTeams_AllRequestsOk_ReturnsCorrectMapping() {
        // 10 ids which we expect to succeed
        var okTeamIds = createTestIds(10);
        // no requests should fail
        List<UUID> failTeamIds = List.of();

        // given
        givenTeamIdsSimulateClientResponses(okTeamIds, failTeamIds, HttpStatus.NOT_FOUND);

        // when
        var teamDetails = asyncMatchServiceClient.getAllTeams(okTeamIds);

        // then
        assertEquals(10, teamDetails.size());
        teamDetails.forEach((key, value) -> assertEquals(key, value.get(0).getId()));
    }

    private static List<UUID> createTestIds(int howMany) {
        return IntStream.range(0, howMany).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toList());
    }

    private static FeignException createTestFeignException(HttpStatus status) {
        String message = "";
        byte[] body = null;
        Map<String, Collection<String>> headers = Map.of();
        var testRequest = Request.create(
                Request.HttpMethod.GET,
                "http://some-test-url.com",
                headers,
                null,
                new RequestTemplate()
        );

        switch (status.value()) {
            case 404:
                return new FeignException.NotFound(message, testRequest, body, headers);
            case 500:
                return new FeignException.InternalServerError(message, testRequest, body, headers);
            default:
                throw new IllegalArgumentException("not implemented for status " + status);
        }
    }

    /**
     * Simulates {@link MatchServiceClient}'s `getTeamById` behavior.
     *
     * @param okIds ids for which we simulate `HTTP 200` responses
     * @param failIds ids for which we simulate error status that's provided in the <i>failureHttpStatus</i>
     * @param failureHttpStatus status code of the responses which simulate failure
     */
    private void givenTeamIdsSimulateClientResponses(List<UUID> okIds, List<UUID> failIds, HttpStatus failureHttpStatus) {
        given(matchServiceClient.getTeamById(any(UUID.class))).willAnswer(inv -> {
            UUID id = inv.getArgument(0);
            if (okIds.contains(id)) {
                return new TeamDetailsDto(id, "Test Team", "");
            } else if (failIds.contains(id)) {
                throw createTestFeignException(failureHttpStatus);
            } else {
                throw new InvalidUseOfMatchersException(
                        String.format("Id %s does not match any of ok/fail ids", id)
                );
            }
        });
    }
}
