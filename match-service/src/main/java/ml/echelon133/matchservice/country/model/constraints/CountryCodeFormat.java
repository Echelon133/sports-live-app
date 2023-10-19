package ml.echelon133.matchservice.country.model.constraints;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CountryCodeFormat.Validator.class)
public @interface CountryCodeFormat {
    String message() default "invalid ISO 3166-1 alpha-2 country code";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<CountryCodeFormat, String> {

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
    }
}
