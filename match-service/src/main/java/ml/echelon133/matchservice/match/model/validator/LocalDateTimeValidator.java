package ml.echelon133.matchservice.match.model.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class LocalDateTimeValidator implements ConstraintValidator<ValidLocalDateTimeFormat, String> {

    private DateTimeFormatter dateTimeFormatter;

    @Override
    public void initialize(ValidLocalDateTimeFormat constraintAnnotation) {
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
