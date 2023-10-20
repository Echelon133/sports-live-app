package ml.echelon133.common.exception;

import java.util.List;
import java.util.Map;

/**
 * Exception thrown when client-provided body of the request does not satisfy all required constraints.
 */
public class RequestBodyContentInvalidException extends Exception {

    private final Map<String, List<String>> validationErrors;

    public RequestBodyContentInvalidException(Map<String, List<String>> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public Map<String, List<String>> getValidationErrors() {
        return validationErrors;
    }
}
