package ml.echelon133.common.exception;

import java.util.List;
import java.util.Map;

/**
 * Exception thrown when user-provided form-like structure is not valid.
 */
public class FormInvalidException extends Exception {

    private final Map<String, List<String>> validationErrors;

    public FormInvalidException(Map<String, List<String>> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public Map<String, List<String>> getValidationErrors() {
        return validationErrors;
    }
}
