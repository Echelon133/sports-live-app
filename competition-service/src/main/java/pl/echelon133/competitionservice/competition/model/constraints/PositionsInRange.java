package pl.echelon133.competitionservice.competition.model.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PositionsInRange.Validator.class)
public @interface PositionsInRange {
    int max() default 24; // maximum size of a group in a competition
    String message() default "expected position numbers between 1 and {max}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<PositionsInRange, Set<Integer>> {

        private int maximumPositionNumber;

        @Override
        public void initialize(PositionsInRange constraintAnnotation) {
            this.maximumPositionNumber = constraintAnnotation.max();
        }

        @Override
        public boolean isValid(Set<Integer> positions, ConstraintValidatorContext constraintValidatorContext) {
            var minimumPositionNumber = 1; // a team cannot have a 0th or a negative position in a group
            return positions.stream()
                    .allMatch(pos -> minimumPositionNumber <= pos && maximumPositionNumber >= pos);
        }
    }
}
