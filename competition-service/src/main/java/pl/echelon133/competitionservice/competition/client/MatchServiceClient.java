package pl.echelon133.competitionservice.competition.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import pl.echelon133.competitionservice.competition.model.TeamDetailsDto;

import java.util.UUID;

@FeignClient(name = "match-service", url = "http://match-service:80")
public interface MatchServiceClient {

    @GetMapping("/api/teams/{teamId}")
    TeamDetailsDto getTeamById(@PathVariable UUID teamId);
}
