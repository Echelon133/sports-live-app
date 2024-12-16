package ml.echelon133.matchservice.event.model.dto.constraints;


import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import ml.echelon133.matchservice.event.model.dto.UpsertGoalEventDto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GoalPlayerIdsDifferent.Validator.class)
public @interface GoalPlayerIdsDifferent {
    String message() default "the id of scoring and assisting player must not be identical";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<GoalPlayerIdsDifferent, UpsertGoalEventDto> {

        @Override
        public boolean isValid(UpsertGoalEventDto goalDto, ConstraintValidatorContext constraintValidatorContext) {
            // only execute the validation logic if the client has actually provided both values, otherwise
            // ignore it and let field validators do their job first
            if (goalDto.scoringPlayerId() == null || goalDto.assistingPlayerId() == null) {
                return true;
            } else {
                // scoringPlayerId and assistingPlayerId are valid when their values are not equal.
                // do not check if these values are actually uuids, since that's the responsibility of field-level validators
                return !goalDto.scoringPlayerId().equals(goalDto.assistingPlayerId());
            }
        }
    }
}
