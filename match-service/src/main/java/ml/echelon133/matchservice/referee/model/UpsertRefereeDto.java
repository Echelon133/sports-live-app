package ml.echelon133.matchservice.referee.model;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotNull;

public record UpsertRefereeDto(@NotNull @Length(min = 1, max = 100) String name) {
}
