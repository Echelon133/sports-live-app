package ml.echelon133.matchservice.match.model.constraints;

import ml.echelon133.matchservice.match.model.UpsertLineupDto;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.stream.Stream;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PlayerIdsUnique.Validator.class)
public @interface PlayerIdsUnique {
    String message() default "at least one team player's id occurs more than once in the lineup";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<PlayerIdsUnique, UpsertLineupDto> {

        @Override
        public boolean isValid(UpsertLineupDto upsertLineupDto, ConstraintValidatorContext constraintValidatorContext) {
            var startingPlayers = upsertLineupDto.getStartingPlayers();
            var substitutePlayers = upsertLineupDto.getSubstitutePlayers();

            // let @NotNull handle these
            if (startingPlayers == null || substitutePlayers == null) {
                return true;
            }

            var size = startingPlayers.size() + substitutePlayers.size();
            var distinctSize = Stream.concat(
                    startingPlayers.stream(), substitutePlayers.stream()
            ).distinct().count();
            return size == distinctSize;
        }
    }
}
