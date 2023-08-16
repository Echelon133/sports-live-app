package ml.echelon133.common.exception;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationResultMapper {

    /**
     * Takes all errors from a {@link BindingResult} and creates a map in which all {@link ObjectError}s are associated
     * with the key {@code general} and all {@link FieldError}s are associated with the keys which are named after the
     * field names.
     *
     * @param result representation of all errors which occurred during the bean validation process
     * @return a map which contains all errors which occurred during the bean validation process
     */
    public static Map<String, List<String>> resultIntoErrorMap(BindingResult result) {
        Map<String, List<String>> mappedResult = new HashMap<>();

        result.getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                var e = (FieldError)error;
                mappedResult.putIfAbsent(e.getField(), new ArrayList<>());
                var fieldErrorList = mappedResult.get(e.getField());
                fieldErrorList.add(e.getDefaultMessage());
            } else {
                mappedResult.putIfAbsent("general", new ArrayList<>());
                var generalErrorList = mappedResult.get("general");
                generalErrorList.add(error.getDefaultMessage());
            }
        });

        return mappedResult;
    }
}
