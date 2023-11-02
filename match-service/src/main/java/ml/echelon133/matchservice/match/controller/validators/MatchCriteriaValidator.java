package ml.echelon133.matchservice.match.controller.validators;

import ml.echelon133.matchservice.match.controller.MatchCriteriaRequestParams;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Ensures that a valid {@link MatchCriteriaRequestParams} maintains these invariants:
 *
 * <ul>
 *     <li>only one of 'date' and 'competitionId' can be provided at a time</li>
 *     <li>if 'date' is provided, then 'utcOffset' might be optionally provided</li>
 *     <li>if 'competitionId' is provided, then 'type' must always be provided</li>
 *     <li>if 'date' is not null, then it can be parsed as {@link LocalDate} with the pattern that's specified in the constructor of this validator</li>
 *     <li>if 'competitionId' is not null, then it can be parsed as {@link UUID}</li>
 *     <li>if 'utcOffset' is not null, then it can be parsed as {@link ZoneOffset}</li>
 *     <li>if 'type' is not null, then it must be a string that contains either 'results' or 'fixtures'</li>
 * </ul>
 */
public class MatchCriteriaValidator implements Validator {

    private String matchDateFormatterPattern;
    private DateTimeFormatter matchDateFormatter;

    private final String DATE_PARAM_NAME = "date";
    private final String UTC_OFFSET_PARAM_NAME = "utcOffset";
    private final String COMPETITION_ID_PARAM_NAME = "competitionId";
    private final String TYPE_PARAM_NAME = "type";

    private final String CANNOT_BE_PROVIDED_TOGETHER_MESSAGE = "cannot be provided together with '%s'";
    private final String NOT_PROVIDED_MESSAGE = "not provided";
    private final String NOT_UUID_MESSAGE = "not a uuid";
    private final String FORMAT_MESSAGE = "format should be %s";
    private final String TYPE_INVALID_MESSAGE = "should be either 'fixtures' or 'results'";

    public MatchCriteriaValidator(String matchDateFormatterPattern) {
        this.matchDateFormatterPattern = matchDateFormatterPattern;
        this.matchDateFormatter = DateTimeFormatter.ofPattern(matchDateFormatterPattern);
    }

    public DateTimeFormatter getMatchDateFormatter() {
        return matchDateFormatter;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return MatchCriteriaRequestParams.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        MatchCriteriaRequestParams obj = (MatchCriteriaRequestParams) target;

        if (obj.getDate() != null && obj.getCompetitionId() != null) {
            // 'date' and 'competitionId' are mutually exclusive, so reject the criteria
            // if both are provided
            errors.rejectValue(
                    DATE_PARAM_NAME,
                    "mutually.exclusive",
                    String.format(CANNOT_BE_PROVIDED_TOGETHER_MESSAGE, COMPETITION_ID_PARAM_NAME)
            );
            errors.rejectValue(
                    COMPETITION_ID_PARAM_NAME,
                    "mutually.exclusive",
                    String.format(CANNOT_BE_PROVIDED_TOGETHER_MESSAGE, DATE_PARAM_NAME)
            );
        } else if (obj.getDate() != null) {
            // date provided, therefore we have to validate the first criteria
            // variant which consists of 'date' and 'utcOffset'

            // check if 'date' can be parsed as LocalDate with specified DateTimeFormatter
            try {
                LocalDate.parse(obj.getDate(), matchDateFormatter);
            } catch (DateTimeException ignore) {
                errors.rejectValue(
                        DATE_PARAM_NAME,
                        "invalid.date.format",
                        String.format(FORMAT_MESSAGE, matchDateFormatterPattern)
                );
            }

            // 'utcOffset' is optional
            // check if 'utcOffset' can be turned into a valid ZoneOffset
            if (obj.getUtcOffset() != null) {
                try {
                    ZoneOffset.of(obj.getUtcOffset());
                } catch (DateTimeException ignore) {
                    errors.rejectValue(
                            UTC_OFFSET_PARAM_NAME,
                            "invalid.utcOffset.format",
                            String.format(FORMAT_MESSAGE, "±hh:mm")
                    );
                }
            }
        } else if (obj.getCompetitionId() != null) {
            // competitionId provided, therefore we have to validate the second criteria
            // variant which consists of 'competitionId' and 'type'

            // check if 'competitionId' can be turned into a valid UUID
            try {
                var ignore = UUID.fromString(obj.getCompetitionId());
            } catch (IllegalArgumentException ignore) {
                errors.rejectValue(COMPETITION_ID_PARAM_NAME, "not.uuid", NOT_UUID_MESSAGE);
            }

            // if 'competitionId' is provided, then 'type' is required and must be equal to
            // either 'fixtures' or 'results'
            String type = obj.getType();
            if (type == null) {
                errors.rejectValue(TYPE_PARAM_NAME, "type.invalid", NOT_PROVIDED_MESSAGE);
            } else if (!(type.equalsIgnoreCase("results") || type.equalsIgnoreCase("fixtures"))) {
                errors.rejectValue(TYPE_PARAM_NAME, "type.invalid", TYPE_INVALID_MESSAGE);
            }
        } else {
            // invalid call - neither 'date' nor 'competitionId' present in the criteria object
            errors.rejectValue(DATE_PARAM_NAME, "not.provided", NOT_PROVIDED_MESSAGE);
            errors.rejectValue(COMPETITION_ID_PARAM_NAME, "not.provided", NOT_PROVIDED_MESSAGE);
        }
    }
}
