package ml.echelon133.matchservice.match.model.validator;

import ml.echelon133.matchservice.match.model.UpsertMatchDto;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class TeamIdsDifferentValidator implements ConstraintValidator<TeamIdsDifferent, UpsertMatchDto> {

    @Override
    public boolean isValid(UpsertMatchDto upsertMatchDto, ConstraintValidatorContext constraintValidatorContext) {
        // only execute the validation logic if the client has actually provided both values, otherwise
        // ignore it and let field validators do their job first
        if (upsertMatchDto.getHomeTeamId() == null || upsertMatchDto.getAwayTeamId() == null) {
            return true;
        } else {
            // homeTeamId and awayTeamId are valid when their values are not equal.
            // do not check if these values are actually uuids, since that's the responsibility of field-level validators
            return !upsertMatchDto.getHomeTeamId().equals(upsertMatchDto.getAwayTeamId());
        }
    }
}
