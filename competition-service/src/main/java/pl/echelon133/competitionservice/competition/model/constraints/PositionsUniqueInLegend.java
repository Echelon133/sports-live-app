package pl.echelon133.competitionservice.competition.model.constraints;

import pl.echelon133.competitionservice.competition.model.UpsertCompetitionDto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.stream.Collectors;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PositionsUniqueInLegend.Validator.class)
public @interface PositionsUniqueInLegend {
    String message() default "multiple legend entries cannot reference the same position";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<PositionsUniqueInLegend, Collection<UpsertCompetitionDto.UpsertLegendDto>> {

        @Override
        public boolean isValid(Collection<UpsertCompetitionDto.UpsertLegendDto> upsertLegendDtos, ConstraintValidatorContext constraintValidatorContext) {
            var allPositionsCounter= upsertLegendDtos.stream()
                    .mapToLong(l -> l.getPositions().size()).sum();
            var uniquePositionsCounter= upsertLegendDtos.stream()
                    .flatMap(l -> l.getPositions().stream()).collect(Collectors.toSet()).size();
            return allPositionsCounter == uniquePositionsCounter;
        }
    }
}
