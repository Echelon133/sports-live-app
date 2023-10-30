package ml.echelon133.matchservice.coach.constraints;

import ml.echelon133.matchservice.coach.repository.CoachRepository;
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
@Constraint(validatedBy = CoachExists.Validator.class)
public @interface CoachExists {
    String message() default "id does not belong to a valid coach";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<CoachExists, String> {

        private CoachRepository coachRepository;

        public Validator() {}

        @Autowired
        public Validator(CoachRepository coachRepository) {
            this.coachRepository = coachRepository;
        }

        @Override
        public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
            if (s == null) {
                return true;
            }
            try {
                return coachRepository.existsByIdAndDeletedFalse(UUID.fromString(s));
            } catch (IllegalArgumentException ignore) {
                return false;
            }
        }
    }
}
