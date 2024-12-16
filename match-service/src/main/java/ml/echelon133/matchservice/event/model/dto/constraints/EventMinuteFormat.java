package ml.echelon133.matchservice.event.model.dto.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EventMinuteFormat.Validator.class)
public @interface EventMinuteFormat {
    String message() default "required minute format: mm(+mm)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<EventMinuteFormat, String> {

        // Accepts three groups
        // * minutes 1-9
        // * minutes 10-120
        // * minutes 45, 90, 120 with extra time notation (e.g. "45+2", "90+5", "120+10")
        private static final Pattern MINUTE_PATTERN = Pattern.compile(
                "(^[1-9]$)|(^[\\d]{2,3}$)|(^(45|90|120)\\+[\\d]{1,2}$)"
        );

        @Override
        public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
            if (s == null) {
                return true;
            }
            return MINUTE_PATTERN.matcher(s).matches();
        }
    }
}
