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
import java.util.Comparator;
import java.util.stream.Collectors;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LegendPositionsInRange.Validator.class)
public @interface LegendPositionsInRange {
    String message() default "legend cannot reference positions which do not exist in groups";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<LegendPositionsInRange, UpsertCompetitionDto.UpsertLeaguePhaseDto> {

        @Override
        public boolean isValid(UpsertCompetitionDto.UpsertLeaguePhaseDto upsertLeaguePhaseDto, ConstraintValidatorContext constraintValidatorContext) {
            if (upsertLeaguePhaseDto == null) {
                return true;
            }

            var minimumLegalPosition = 1; // a team cannot have a 0th or a negative position in a group
            var maximumLegalPosition = upsertLeaguePhaseDto.groups().stream()
                    .map(s -> s.teams().size())
                    .max(Comparator.naturalOrder());

            // this can only happen if there aren't any groups, or the groups do not have any teams,
            // which is handled by other validators (e.g. triggered by @Size),
            // therefore exiting early is ok
            if (maximumLegalPosition.isEmpty()) {
                return true;
            }

            var allPositionReferences = upsertLeaguePhaseDto.legend().stream()
                    .flatMap(l -> l.positions().stream())
                    .collect(Collectors.toSet());

            return allPositionReferences.stream()
                    .allMatch(pos -> minimumLegalPosition <= pos && maximumLegalPosition.get() >= pos);
        }
    }
}
