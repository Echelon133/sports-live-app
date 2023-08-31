package ml.echelon133.matchservice.player.model.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PositionValidator implements ConstraintValidator<ValidPositionValue, String> {

    private List<String> acceptedValues;

    @Override
    public void initialize(ValidPositionValue constraintAnnotation) {
        this.acceptedValues =
                Arrays.stream(constraintAnnotation.acceptedPositions()).map(Enum::name).collect(Collectors.toList());
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if (s == null) {
            // let @NotNull do its job
            return true;
        } else {
            return acceptedValues.contains(s.toUpperCase());
        }
    }
}
