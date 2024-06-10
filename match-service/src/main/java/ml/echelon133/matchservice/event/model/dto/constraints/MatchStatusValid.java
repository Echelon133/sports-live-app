package ml.echelon133.matchservice.event.model.dto.constraints;

import ml.echelon133.common.match.MatchStatus;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MatchStatusValid.Validator.class)
public @interface MatchStatusValid {
    String message() default "provided status is invalid";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<MatchStatusValid, String> {

        @Override
        public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
            if (s == null) {
                return true;
            }
            try {
                MatchStatus.valueOfIgnoreCase(s);
                return true;
            } catch (IllegalArgumentException ignore) {
                return false;
            }
        }
    }
}
