package ml.echelon133.matchservice.player.model.validator;


import ml.echelon133.matchservice.player.service.PlayerService;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LocalDateValidator.class)
public @interface ValidLocalDateFormat {
    String dateFormat() default PlayerService.DATE_OF_BIRTH_FORMAT;
    String message() default "required date format {dateFormat}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
