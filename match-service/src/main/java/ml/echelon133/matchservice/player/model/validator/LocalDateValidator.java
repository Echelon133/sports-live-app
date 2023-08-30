package ml.echelon133.matchservice.player.model.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class LocalDateValidator implements ConstraintValidator<ValidLocalDateFormat, String> {

    private DateTimeFormatter dateTimeFormatter;

    @Override
    public void initialize(ValidLocalDateFormat constraintAnnotation) {
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
