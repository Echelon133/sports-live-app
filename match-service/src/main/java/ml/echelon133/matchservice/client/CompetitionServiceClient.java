package ml.echelon133.matchservice.client;

import ml.echelon133.matchservice.match.model.CompetitionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "competition-service", url = "http://competition-service:80")
public interface CompetitionServiceClient {

    @GetMapping("/api/competitions/{competitionId}")
    CompetitionDto getCompetitionById(@PathVariable UUID competitionId);
}
