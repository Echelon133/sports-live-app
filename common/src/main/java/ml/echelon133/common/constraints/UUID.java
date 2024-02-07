package ml.echelon133.common.constraints;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE})
@Constraint(validatedBy = UUID.Validator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface UUID {
    String message() default "not a valid uuid";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<UUID, String> {

        @Override
        public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
            if (s == null) {
                return true;
            }

            try {
                java.util.UUID.fromString(s);
                return true;
            } catch (IllegalArgumentException ignore) {
                return false;
            }
        }
    }
}
