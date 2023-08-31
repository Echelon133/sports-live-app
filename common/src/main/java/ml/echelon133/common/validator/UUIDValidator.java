package ml.echelon133.common.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.UUID;

public class UUIDValidator implements ConstraintValidator<ValidUUID, String> {

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
