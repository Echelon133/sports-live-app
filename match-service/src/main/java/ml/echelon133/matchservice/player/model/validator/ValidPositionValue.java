package ml.echelon133.matchservice.player.model.validator;

import ml.echelon133.matchservice.player.model.Position;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PositionValidator.class)
public @interface ValidPositionValue {
    Position[] acceptedPositions() default {Position.GOALKEEPER, Position.DEFENDER, Position.MIDFIELDER, Position.FORWARD};
    String message() default "required exactly one of {acceptedPositions}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
