package pl.echelon133.competitionservice.competition.model.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import pl.echelon133.competitionservice.competition.model.UpsertCompetitionDto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AtLeastOnePhaseNotNull.Validator.class)
public @interface AtLeastOnePhaseNotNull {
    String message() default "a competition must have at least one phase";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<AtLeastOnePhaseNotNull, UpsertCompetitionDto> {

        @Override
        public boolean isValid(UpsertCompetitionDto upsertCompetitionDto, ConstraintValidatorContext constraintValidatorContext) {
            return !(upsertCompetitionDto.leaguePhase() == null && upsertCompetitionDto.knockoutPhase() == null);
        }
    }
}
