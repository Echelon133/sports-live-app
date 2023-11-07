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
     * Takes all errors from a {@link BindingResult} and creates a map in which:
     * <ul>
     *     <li>each {@link ObjectError}'s message is placed in a map, associated with the 'general' key</li>
     *     <li>each {@link FieldError}'s message is placed in a map, associated with the key being that field's name</li>
     * </ul>
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

    /**
     * Takes all field errors from a {@link BindingResult} which validated request parameters
     * and creates a map in which field names of all {@link FieldError}s are associated with
     * the default messages of these errors.
     *
     * <p>
     *     A field error of field 'test' with a message 'asdf' will end up as the equivalent of
     *     {@code Map.of("test", "asdf")}
     * </p>
     *
     * @param result representation of all field errors which occurred during the bean validation process
     * @return a map which contains all field errors which occurred during the bean validation process
     */
    public static Map<String, String> requestParamResultIntoErrorMap(BindingResult result) {
        Map<String, String> mappedResult = new HashMap<>();
        // when we validate request params, we only use FieldErrors
        result.getFieldErrors().forEach(fieldError -> {
            mappedResult.put(fieldError.getField(), fieldError.getDefaultMessage());
        });
        return mappedResult;
    }
}
