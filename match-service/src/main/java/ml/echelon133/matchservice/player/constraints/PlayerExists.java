package ml.echelon133.matchservice.player.constraints;

import ml.echelon133.matchservice.player.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PlayerExists.Validator.class)
public @interface PlayerExists {
    String message() default "id does not belong to a valid player";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<PlayerExists, String> {

        private PlayerRepository playerRepository;

        public Validator() {}

        @Autowired
        public Validator(PlayerRepository playerRepository) {
            this.playerRepository = playerRepository;
        }

        @Override
        public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
            if (s == null) {
                return true;
            }
            try {
                return playerRepository.existsByIdAndDeletedFalse(UUID.fromString(s));
            } catch (IllegalArgumentException ignore) {
                return false;
            }
        }
    }
}
