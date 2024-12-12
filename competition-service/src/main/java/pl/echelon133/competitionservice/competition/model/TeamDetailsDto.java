package pl.echelon133.competitionservice.competition.model;

import java.util.UUID;

public record TeamDetailsDto(UUID id, String name, String crestUrl) {
}
