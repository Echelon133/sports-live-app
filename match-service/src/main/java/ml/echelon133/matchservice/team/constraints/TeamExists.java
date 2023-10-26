package ml.echelon133.matchservice.team.constraints;

import ml.echelon133.matchservice.team.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TeamExists.Validator.class)
public @interface TeamExists {
    String message() default "id does not belong to a valid team";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<TeamExists, String> {

        private TeamRepository teamRepository;

        public Validator() {}

        @Autowired
        public Validator(TeamRepository teamRepository) {
            this.teamRepository = teamRepository;
        }

        @Override
        public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
            if (s == null) {
                return true;
            }
            try {
                return teamRepository.existsByIdAndDeletedFalse(UUID.fromString(s));
            } catch (IllegalArgumentException ignore) {
                return false;
            }
        }
    }
}
