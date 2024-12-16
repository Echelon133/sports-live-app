package ml.echelon133.matchservice.venue.constraints;

import ml.echelon133.matchservice.venue.repository.VenueRepository;
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
@Constraint(validatedBy = VenueExists.Validator.class)
public @interface VenueExists {
    String message() default "id does not belong to a valid venue";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<VenueExists, String> {

        private VenueRepository venueRepository;

        public Validator() {}

        @Autowired
        public Validator(VenueRepository venueRepository) {
            this.venueRepository = venueRepository;
        }

        @Override
        public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
            if (s == null) {
                return true;
            }
            try {
                return venueRepository.existsByIdAndDeletedFalse(UUID.fromString(s));
            } catch (IllegalArgumentException ignore) {
                return false;
            }
        }
    }
}
