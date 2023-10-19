package ml.echelon133.matchservice.player.model.validator;

import ml.echelon133.matchservice.player.model.Position;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPositionValue.Validator.class)
public @interface ValidPositionValue {
    Position[] acceptedPositions() default {Position.GOALKEEPER, Position.DEFENDER, Position.MIDFIELDER, Position.FORWARD};
    String message() default "required exactly one of {acceptedPositions}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidPositionValue, String> {

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
}
