package ml.echelon133.matchservice.match.model.validator;

import ml.echelon133.matchservice.match.service.MatchService;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LocalDateTimeValidator.class)
public @interface ValidLocalDateTimeFormat {
    String dateFormat() default MatchService.DATE_OF_MATCH_FORMAT;
    String message() default "required date format {dateFormat}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
