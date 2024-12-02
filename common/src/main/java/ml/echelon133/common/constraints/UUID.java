package ml.echelon133.common.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

@Target({ElementType.FIELD, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE})
@Constraint(validatedBy = UUID.Validator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface UUID {
    String message() default "not a valid uuid";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<UUID, String> {

        private static Pattern UUID_REGEX = Pattern.compile(
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        );

        @Override
        public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
            if (s == null) {
                return true;
            }

            return UUID_REGEX.matcher(s).matches();
        }
    }
}
