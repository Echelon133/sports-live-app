package ml.echelon133.matchservice.match.model.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.regex.Pattern;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FormationCorrect.Validator.class)
public @interface FormationCorrect {
    String message() default "provided formation is invalid";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<FormationCorrect, String> {

        // regex for initial verification of the formation's format:
        //      * should start with a digit in the 1-5 range
        //      * repeats a group consisting of a single hyphen, followed by a digit in the 1-5 range
        //              (group repeats at least 2 times, at most 4 times)
        //
        // an input which satisfies this regex needs further validation, since an example input such as "5-5-5"
        // is a match, despite the fact that it refers to 15 players, when it must refer exactly to 10 players
        private static final Pattern FORMATION_PATTERN = Pattern.compile("^([1-5])(-[1-5]){2,4}$");

        @Override
        public boolean isValid(String formation, ConstraintValidatorContext constraintValidatorContext) {
            // let @NotNull handle this case if it's present
            if (formation == null) {
                return true;
            }

            // a formation string is valid if:
            //      * it matches the regex
            //      * it refers to exactly 10 players
            if (FORMATION_PATTERN.matcher(formation).matches()) {
                // split formation string around hyphens, e.g. 4-4-2 should become {"4", "4", "2"}
                String[] formationRows = formation.split("-");

                // the formation string should refer to EXACTLY 10 players (the goalkeeper's position is implicit)
                var playerCount = Arrays.stream(formationRows)
                        .map(Integer::valueOf)
                        .reduce(0, Integer::sum);

                return playerCount == 10;
            }

            return false;
        }
    }
}
