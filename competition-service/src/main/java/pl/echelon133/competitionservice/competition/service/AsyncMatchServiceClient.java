package pl.echelon133.competitionservice.competition.service;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.echelon133.competitionservice.competition.client.MatchServiceClient;
import pl.echelon133.competitionservice.competition.model.TeamDetailsDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class AsyncMatchServiceClient {

    private final MatchServiceClient matchServiceClient;
    private final Executor asyncExecutor;
    private final Logger logger = LoggerFactory.getLogger(AsyncMatchServiceClient.class);

    /** Wrapper which lets us create {@link CompletionException} which does
     * not append "java.util.concurrent.CompletionException" at the start of the exception message.
     * The constructor which only accepts a {@link String} is protected and can only be used through
     * a subtype.
     * This is needed because the message of this exception is sent to the client whose request
     * has not completed successfully.
     */
    public static class FetchFailedException extends CompletionException {
        public FetchFailedException(UUID id) {
            super(String.format("failed to fetch resource with id %s", id));
        }
    }

    @Autowired
    public AsyncMatchServiceClient(MatchServiceClient matchServiceClient, Executor asyncExecutor) {
        this.matchServiceClient = matchServiceClient;
        this.asyncExecutor = asyncExecutor;
        logger.info("Initializing with executor " + asyncExecutor.toString());
    }

    private CompletableFuture<TeamDetailsDto> getTeamById(UUID teamId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var response = matchServiceClient.getTeamById(teamId);
                logger.debug("Async fetch of team with id {} successful", teamId);
                return response;
            } catch (FeignException ex) {
                logger.debug("Async fetch of team with id {} failed with status {}", teamId, ex.status());
                throw new FetchFailedException(teamId);
            }
        }, asyncExecutor);
    }

    /**
     * Asynchronously fetches {@link TeamDetailsDto} of each team whose {@link UUID} is provided.
     *
     * @param teamIds a list of ids of teams to fetch
     * @return a mapping between UUIDs and fetched details about a team
     * @throws CompletionException thrown when at least a single team's info could not be fetched for some reason
     */
    public Map<UUID, List<TeamDetailsDto>> getAllTeams(List<UUID> teamIds) throws CompletionException {
        return teamIds.stream().parallel()
                .map(this::getTeamById)
                .map(CompletableFuture::join)
                .collect(Collectors.groupingBy(TeamDetailsDto::getId));
    }
}
