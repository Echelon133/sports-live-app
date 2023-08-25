package ml.echelon133.matchservice.country.model.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class CountryCodeValidator implements ConstraintValidator<ValidCountryCode, String> {

    // https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2#Decoding_table
    // Only check if the flag is
    private static final Pattern FLAG_CODE_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z]");

    @Override
    public boolean isValid(String flagCode, ConstraintValidatorContext constraintValidatorContext) {
        if (flagCode != null) {
            return FLAG_CODE_PATTERN.matcher(flagCode).matches();
        }
        return false;
    }

    @Override
    public void initialize(ValidCountryCode constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }
}
