package pl.echelon133.competitionservice.competition.model.constraints;

import pl.echelon133.competitionservice.competition.model.Legend;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SentimentValue.Validator.class)
public @interface SentimentValue {
    Legend.LegendSentiment[] acceptedSentiments() default {
        Legend.LegendSentiment.POSITIVE_A, Legend.LegendSentiment.POSITIVE_B,
        Legend.LegendSentiment.POSITIVE_C, Legend.LegendSentiment.POSITIVE_D,
        Legend.LegendSentiment.NEGATIVE_A, Legend.LegendSentiment.NEGATIVE_B
    };
    String message() default "required exactly one of {acceptedSentiments}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<SentimentValue, String> {

        private List<String> acceptedValues;

        @Override
        public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
            if (s == null) {
                // let @NotNull do its job
                return true;
            } else {
                return acceptedValues.contains(s.toUpperCase());
            }
        }

        @Override
        public void initialize(SentimentValue constraintAnnotation) {
            this.acceptedValues =
                    Arrays.stream(constraintAnnotation.acceptedSentiments()).map(Enum::name).collect(Collectors.toList());
        }
    }
}
