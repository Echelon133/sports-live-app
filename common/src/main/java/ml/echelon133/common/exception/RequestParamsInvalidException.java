package ml.echelon133.common.exception;

import java.util.Map;

public class RequestParamsInvalidException extends Exception {

    private final Map<String, String> validationErrors;

    public RequestParamsInvalidException(Map<String, String> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }
}
