package ml.echelon133.matchservice.country.model.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CountryCodeValidator.class)
public @interface ValidCountryCode {
    String message() default "invalid ISO 3166-1 alpha-2 country code";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
