package ml.echelon133.matchservice.event.model.dto.constraints;

import ml.echelon133.matchservice.event.model.dto.InsertMatchEvent;

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
@Constraint(validatedBy = SubstitutionPlayerIdsDifferent.Validator.class)
public @interface SubstitutionPlayerIdsDifferent {
    String message() default "the id of in and out players must not be identical";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<SubstitutionPlayerIdsDifferent, InsertMatchEvent.SubstitutionDto> {

        @Override
        public boolean isValid(InsertMatchEvent.SubstitutionDto substitutionDto, ConstraintValidatorContext constraintValidatorContext) {
            // only execute the validation logic if the client has actually provided both values, otherwise
            // ignore it and let field validators do their job first
            if (substitutionDto.getPlayerInId() == null || substitutionDto.getPlayerOutId() == null) {
                return true;
            } else {
                // playerInId and playerOutId are valid when their values are not equal.
                // do not check if these values are actually uuids, since that's the responsibility of field-level validators
                return !substitutionDto.getPlayerInId().equals(substitutionDto.getPlayerOutId());
            }
        }
    }
}
