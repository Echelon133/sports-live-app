package ml.echelon133.matchservice.match.model.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TeamIdsDifferentValidator.class)
public @interface TeamIdsDifferent {
    String message() default "homeTeamId and awayTeamId must not be identical";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
