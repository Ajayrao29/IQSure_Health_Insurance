
package org.hartford.iqsure.exception;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(404, "Not Found", ex.getMessage(), LocalDateTime.now()));
    }
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(400, "Bad Request", ex.getMessage(), LocalDateTime.now()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(errors);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(500, "Internal Server Error", ex.getMessage(), LocalDateTime.now()));
    }
}