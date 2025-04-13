package pl.echelon133.competitionservice.competition.model.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import pl.echelon133.competitionservice.competition.model.KnockoutStage;
import pl.echelon133.competitionservice.competition.model.UpsertKnockoutTreeDto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StageSlotSizeExact.Validator.class)
public @interface StageSlotSizeExact {
    String message() default "stage {stageName} must contain exactly {slotSize} slots";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<StageSlotSizeExact, UpsertKnockoutTreeDto.UpsertStage> {
        @Override
        public boolean isValid(
                UpsertKnockoutTreeDto.UpsertStage upsertStage,
                ConstraintValidatorContext constraintValidatorContext
        ) {
            if (upsertStage.stage() == null || upsertStage.slots() == null) {
                return true;
            }
            try {
                KnockoutStage stage = KnockoutStage.valueOfIgnoreCase(upsertStage.stage());
                boolean valid = stage.getSlots() == upsertStage.slots().size();
                if (!valid) {
                    var context = ((ConstraintValidatorContextImpl) constraintValidatorContext);
                    context.addMessageParameter("stageName", stage.name());
                    context.addMessageParameter("slotSize", stage.getSlots());
                }
                return valid;
            } catch (IllegalArgumentException ignore) {
                // skip this validator, since the stage name is not a valid KnockoutStage enum value,
                // and it's the job of a different validator to handle exactly that case
                return true;
            }
        }
    }
}
