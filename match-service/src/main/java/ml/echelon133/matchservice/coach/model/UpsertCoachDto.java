package ml.echelon133.matchservice.coach.model;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotNull;

public record UpsertCoachDto(@NotNull @Length(min = 1, max = 100) String name) {
}
