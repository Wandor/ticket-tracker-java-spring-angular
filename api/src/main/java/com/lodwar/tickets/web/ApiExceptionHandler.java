package com.lodwar.tickets.web;

import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Renders every error as {"error": {"code", "message", "fields"?}}. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> invalidBody(MethodArgumentNotValidException e) {
        Map<String, String> fields = new TreeMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return validation(fields);
    }

    @ExceptionHandler(ValidationException.class)
    ResponseEntity<Map<String, Object>> invalidInput(ValidationException e) {
        return validation(e.fields());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_json", "Request body is not valid JSON", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<Map<String, Object>> mediaType(HttpMediaTypeNotSupportedException e) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported_media_type", "Send application/json", null);
    }

    @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class})
    ResponseEntity<Map<String, Object>> notFound(Exception e) {
        String message = e instanceof NotFoundException ? e.getMessage() : "Not found";
        return error(HttpStatus.NOT_FOUND, "not_found", message, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Map<String, Object>> methodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "method_not_allowed", e.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> unexpected(Exception e) {
        log.error("Unhandled error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Something went wrong", null);
    }

    private static ResponseEntity<Map<String, Object>> validation(Map<String, String> fields) {
        return error(HttpStatus.BAD_REQUEST, "validation_error", "Request validation failed", fields);
    }

    private static ResponseEntity<Map<String, Object>> error(
            HttpStatus status, String code, String message, Map<String, String> fields) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        if (fields != null && !fields.isEmpty()) {
            body.put("fields", fields);
        }
        return ResponseEntity.status(status).body(Map.of("error", body));
    }
}
