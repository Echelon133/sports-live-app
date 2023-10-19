package ml.echelon133.common.validator;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;

@Target(ElementType.FIELD)
@Constraint(validatedBy = ValidUUID.Validator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUUID {
    String message() default "not a valid uuid";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidUUID, String> {

        @Override
        public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
            if (s == null) {
                return true;
            }

            try {
                var ignore = UUID.fromString(s);
                return true;
            } catch (IllegalArgumentException ignore) {
                return false;
            }
        }
    }
}
