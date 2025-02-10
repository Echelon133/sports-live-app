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
import java.util.Set;
import java.util.stream.Collectors;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StageNamesUnique.Validator.class)
public @interface StageNamesUnique {
    String message() default "stage names cannot repeat in a single knockout tree";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<StageNamesUnique, UpsertKnockoutTreeDto> {
        @Override
        public boolean isValid(UpsertKnockoutTreeDto upsertKnockoutTreeDto, ConstraintValidatorContext constraintValidatorContext) {
            if (upsertKnockoutTreeDto.stages() == null) {
                return true;
            }
            Set<String> uniqueStageNames = upsertKnockoutTreeDto
                    .stages()
                    .stream().filter(s -> s.stage() != null)
                    .map(s -> s.stage().toLowerCase())
                    .collect(Collectors.toSet());
            // if there are as many unique stage names as there are stages, there are no duplicates
            return uniqueStageNames.size() == upsertKnockoutTreeDto.stages().size();
        }
    }
}
