package pl.echelon133.competitionservice.competition.model.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import pl.echelon133.competitionservice.competition.model.KnockoutStage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = KnockoutStageValue.Validator.class)
public @interface KnockoutStageValue {
    KnockoutStage[] acceptedStages() default {
        KnockoutStage.ROUND_OF_128, KnockoutStage.ROUND_OF_64,
        KnockoutStage.ROUND_OF_32, KnockoutStage.ROUND_OF_16,
        KnockoutStage.QUARTER_FINAL, KnockoutStage.SEMI_FINAL,
        KnockoutStage.FINAL
    };
    String message() default "require exactly one of {acceptedStages}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<KnockoutStageValue, String> {

        private List<String> acceptedValues;

        @Override
        public void initialize(KnockoutStageValue constraintAnnotation) {
            this.acceptedValues =
                    Arrays.stream(constraintAnnotation.acceptedStages()).map(Enum::name).collect(Collectors.toList());
        }

        @Override
        public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
            if (s == null) {
                return true;
            }
            return acceptedValues.contains(s.toUpperCase());
        }
    }
}
