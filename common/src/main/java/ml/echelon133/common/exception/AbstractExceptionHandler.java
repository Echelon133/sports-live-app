package ml.echelon133.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public abstract class AbstractExceptionHandler extends ResponseEntityExceptionHandler {

    private static class BaseErrorMessage<T> {
        protected Date timestamp;
        protected HttpStatus status;
        protected String path;
        protected T messages;

        public BaseErrorMessage(HttpStatus status, WebRequest request, T messages) {
            this.timestamp = new Date();
            this.path = ((ServletWebRequest)request).getRequest().getRequestURI();
            this.status = status;
            this.messages = messages;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public String getPath() {
            return path;
        }

        public Integer getStatus() {
            return status.value();
        }

        public String getError() {
            return status.getReasonPhrase();
        }

        public T getMessages() {
            return messages;
        }
    }

    public static final class ErrorMessage extends BaseErrorMessage<List<String>> {
        public ErrorMessage(HttpStatus status, WebRequest request, List<String> messages) {
            super(status, request, messages);
        }

        public ErrorMessage(HttpStatus status, WebRequest request, String... messages) {
            this(status, request, Arrays.asList(messages));
        }

        public ResponseEntity<ErrorMessage> asResponseEntity() {
            return new ResponseEntity<>(this, status);
        }
    }

    public static final class MapErrorMessage extends BaseErrorMessage<Map<String, List<String>>> {
        public MapErrorMessage(HttpStatus status, WebRequest request, Map<String, List<String>> messages) {
            super(status, request, messages);
        }

        public ResponseEntity<MapErrorMessage> asResponseEntity() {
            return new ResponseEntity<>(this, status);
        }
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    protected ResponseEntity<ErrorMessage> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.BAD_REQUEST, request, ex.getMessage());
        return error.asResponseEntity();
    }

    @ExceptionHandler(value = ResourceNotFoundException.class)
    protected ResponseEntity<ErrorMessage> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.NOT_FOUND, request, ex.getMessage());
        return error.asResponseEntity();
    }

    @ExceptionHandler(value = FormInvalidException.class)
    protected ResponseEntity<MapErrorMessage> handleFormInvalidException(FormInvalidException ex, WebRequest request) {
        MapErrorMessage error = new MapErrorMessage(HttpStatus.UNPROCESSABLE_ENTITY, request, ex.getValidationErrors());
        return error.asResponseEntity();
    }
}
