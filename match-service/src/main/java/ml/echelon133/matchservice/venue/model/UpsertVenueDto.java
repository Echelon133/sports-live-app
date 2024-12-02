package ml.echelon133.matchservice.venue.model;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpsertVenueDto(
   @NotNull @Length(min = 1, max = 120) String name,
   @Min(value = 0, message = "expected positive integers") Integer capacity
) {}
