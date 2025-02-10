package pl.echelon133.competitionservice.competition.model.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import pl.echelon133.competitionservice.competition.model.UpsertKnockoutTreeDto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MatchesUnique.Validator.class)
public @interface MatchesUnique {
    String message() default "matches cannot repeat in a single knockout tree";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<MatchesUnique, UpsertKnockoutTreeDto> {
        @Override
        public boolean isValid(
                UpsertKnockoutTreeDto upsertKnockoutTreeDto,
                ConstraintValidatorContext constraintValidatorContext
        ) {
            if (upsertKnockoutTreeDto.stages() == null) {
                return true;
            }

            List<UUID> matchIds = new ArrayList<>(32);
            // traverse the entire knockout tree looking for matchIds contained within TAKEN slots
            // there is at most 7 stages (with sizes 64, 32, 16, 8, 4, 2, 1), so in the worst case
            // (i.e. 7 stages, all of them full, with both legs present) there will be 254 matchIds
            for (var stage : upsertKnockoutTreeDto.stages()) {
                if (stage.slots() == null) {
                    continue;
                }
                for (var slot : stage.slots()) {
                    if (slot instanceof UpsertKnockoutTreeDto.Taken taken) {
                        if (taken.firstLeg() != null) {
                            matchIds.add(taken.firstLeg());
                        }
                        if (taken.secondLeg() != null) {
                            matchIds.add(taken.secondLeg());
                        }
                    }
                }
            }
            return matchIds.size() == new HashSet<>(matchIds).size();
        }
    }
}
