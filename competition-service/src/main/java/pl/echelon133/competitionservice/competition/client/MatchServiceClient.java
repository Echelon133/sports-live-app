package pl.echelon133.competitionservice.competition.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.echelon133.competitionservice.competition.model.TeamDetailsDto;
import pl.echelon133.competitionservice.competition.model.UnassignedMatchDto;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "match-service", url = "http://match-service:80")
public interface MatchServiceClient {

    @GetMapping("/api/teams")
    Page<TeamDetailsDto> getTeamByTeamIds(@RequestParam List<UUID> teamIds, Pageable pageable);

    @GetMapping("/api/matches")
    List<UnassignedMatchDto> getMatchesById(@RequestParam List<UUID> matchIds);
}
