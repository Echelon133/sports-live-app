package ml.echelon133.matchservice.match.controller.validators;

import ml.echelon133.matchservice.match.controller.MatchCriteriaRequestParams;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class MatchCriteriaValidator implements Validator {

    private String matchDateFormatterPattern;
    private DateTimeFormatter matchDateFormatter;

    private final String DATE_PARAM_NAME = "date";
    private final String UTC_OFFSET_PARAM_NAME = "utcOffset";

    private final String NOT_PROVIDED_MESSAGE = "not provided";
    private final String FORMAT_MESSAGE = "format should be %s";

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

        // check if 'date' can be parsed as LocalDate with specified DateTimeFormatter
        try {
            LocalDate.parse(obj.date(), matchDateFormatter);
        } catch (DateTimeException ignore) {
            errors.rejectValue(
                    DATE_PARAM_NAME,
                    "invalid.date.format",
                    String.format(FORMAT_MESSAGE, matchDateFormatterPattern)
            );
        }

        // 'utcOffset' is optional
        // check if 'utcOffset' can be turned into a valid ZoneOffset
        if (obj.utcOffset() != null) {
            try {
                ZoneOffset.of(obj.utcOffset());
            } catch (DateTimeException ignore) {
                errors.rejectValue(
                        UTC_OFFSET_PARAM_NAME,
                        "invalid.utcOffset.format",
                        String.format(FORMAT_MESSAGE, "±hh:mm")
                );
            }
        }
    }
}