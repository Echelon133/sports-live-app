package ml.echelon133.matchservice.referee.constraints;

import ml.echelon133.matchservice.referee.repository.RefereeRepository;
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
@Constraint(validatedBy = RefereeExists.Validator.class)
public @interface RefereeExists {
    String message() default "id does not belong to a valid referee";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<RefereeExists, String> {

        private RefereeRepository refereeRepository;

        public Validator() {}

        @Autowired
        public Validator(RefereeRepository refereeRepository) {
            this.refereeRepository = refereeRepository;
        }

        @Override
        public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
            if (s == null) {
                return true;
            }
            try {
                return refereeRepository.existsByIdAndDeletedFalse(UUID.fromString(s));
            } catch (IllegalArgumentException ignore) {
                return false;
            }
        }
    }

}
