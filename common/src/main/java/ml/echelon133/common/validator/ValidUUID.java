package ml.echelon133.common.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Constraint(validatedBy = UUIDValidator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUUID {
    String message() default "not a valid uuid";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
