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
@Constraint(validatedBy = TeamsUniqueInGroups.Validator.class)
public @interface TeamsUniqueInGroups {
    String message() default "team cannot be a member of multiple groups in one competition";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<TeamsUniqueInGroups, Collection<UpsertCompetitionDto.UpsertGroupDto>> {

        @Override
        public boolean isValid(Collection<UpsertCompetitionDto.UpsertGroupDto> upsertGroupDtos, ConstraintValidatorContext constraintValidatorContext) {
            var allIdsCounter = upsertGroupDtos.stream()
                    .flatMap(g -> g.getTeams().stream()).count();
            var uniqueIdsCounter = upsertGroupDtos.stream()
                    .flatMap(g -> g.getTeams().stream()).collect(Collectors.toSet()).size();
            return allIdsCounter == uniqueIdsCounter;
        }
    }
}
