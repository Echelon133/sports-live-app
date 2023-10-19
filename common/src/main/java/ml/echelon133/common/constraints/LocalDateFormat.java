package ml.echelon133.common.constraints;


import ml.echelon133.common.constants.DateFormatConstants;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LocalDateFormat.Validator.class)
public @interface LocalDateFormat {
    String dateFormat() default DateFormatConstants.DATE_FORMAT;
    String message() default "required date format {dateFormat}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<LocalDateFormat, String> {

        private DateTimeFormatter dateTimeFormatter;

        @Override
        public void initialize(LocalDateFormat constraintAnnotation) {
            this.dateTimeFormatter = DateTimeFormatter.ofPattern(constraintAnnotation.dateFormat());
        }

        @Override
        public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
            if (s == null) {
                // let @NotNull do its job
                return true;
            }

            try {
                LocalDate.parse(s, dateTimeFormatter);
                return true;
            } catch (DateTimeParseException ignore) {
                return false;
            }
        }
    }
}
