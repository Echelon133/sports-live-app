package ml.echelon133.matchservice.match.model.constraints;

import ml.echelon133.matchservice.match.model.UpsertMatchDto;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TeamIdsDifferent.Validator.class)
public @interface TeamIdsDifferent {
    String message() default "homeTeamId and awayTeamId must not be identical";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<TeamIdsDifferent, UpsertMatchDto> {

        @Override
        public boolean isValid(UpsertMatchDto upsertMatchDto, ConstraintValidatorContext constraintValidatorContext) {
            // only execute the validation logic if the client has actually provided both values, otherwise
            // ignore it and let field validators do their job first
            if (upsertMatchDto.getHomeTeamId() == null || upsertMatchDto.getAwayTeamId() == null) {
                return true;
            } else {
                // homeTeamId and awayTeamId are valid when their values are not equal.
                // do not check if these values are actually uuids, since that's the responsibility of field-level validators
                return !upsertMatchDto.getHomeTeamId().equals(upsertMatchDto.getAwayTeamId());
            }
        }
    }
}
