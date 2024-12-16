package ml.echelon133.matchservice.team.constraints;

import ml.echelon133.matchservice.team.repository.TeamPlayerRepository;
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

@Target({ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TeamPlayerExists.Validator.class)
public @interface TeamPlayerExists {
    String message() default "id does not belong to a valid team player";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<TeamPlayerExists, String> {

        private TeamPlayerRepository teamPlayerRepository;

        public Validator() {}

        @Autowired
        public Validator(TeamPlayerRepository teamPlayerRepository) {
            this.teamPlayerRepository = teamPlayerRepository;
        }

        @Override
        public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
            if (s == null) {
                return true;
            }
            try {
                return teamPlayerRepository.existsByIdAndDeletedFalse(UUID.fromString(s));
            } catch (IllegalArgumentException ignore){
                return false;
            }
        }
    }
}
