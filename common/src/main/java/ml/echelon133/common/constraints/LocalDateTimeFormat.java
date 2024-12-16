package ml.echelon133.common.constraints;

import ml.echelon133.common.constants.DateFormatConstants;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LocalDateTimeFormat.Validator.class)
public @interface LocalDateTimeFormat {
    String dateFormat() default DateFormatConstants.DATE_TIME_FORMAT;
    String message() default "required date format {dateFormat}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<LocalDateTimeFormat, String> {

        private DateTimeFormatter dateTimeFormatter;

        @Override
        public void initialize(LocalDateTimeFormat constraintAnnotation) {
            this.dateTimeFormatter = DateTimeFormatter.ofPattern(constraintAnnotation.dateFormat());
        }

        @Override
        public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
            if (s == null) {
                // let @NotNull do its job
                return true;
            }

            try {
                LocalDateTime.parse(s, dateTimeFormatter);
                return true;
            } catch (DateTimeParseException ignore) {
                return false;
            }
        }
    }
}
