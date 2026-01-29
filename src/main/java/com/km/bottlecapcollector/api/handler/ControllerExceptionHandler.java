package com.km.bottlecapcollector.api.handler;

import com.km.bottlecapcollector.api.handler.exception.AppBadRequestException;
import com.km.bottlecapcollector.api.handler.exception.AppForbiddenException;
import com.km.bottlecapcollector.api.handler.exception.AppResourceNotFoundException;
import com.km.bottlecapcollector.api.handler.exception.AppValidationException;
import com.km.bottlecapcollector.api.handler.exception.RateLimitExceededException;
import com.km.bottlecapcollector.api.model.response.ErrorResponse;
import com.km.bottlecapcollector.cloud.storage.CloudStorageException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;


@ControllerAdvice
@Slf4j
public class ControllerExceptionHandler {

    @ExceptionHandler(AppResourceNotFoundException.class)
    public ResponseEntity<@NotNull ErrorResponse> handleAppResourceNotFoundException(AppResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.create(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(AppBadRequestException.class)
    public ResponseEntity<@NotNull ErrorResponse> handleAppBadRequestException(AppBadRequestException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.create(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()));
    }

    @ExceptionHandler(AppForbiddenException.class)
    public ResponseEntity<@NotNull ErrorResponse> handleAppForbiddenException(AppForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.create(HttpStatus.FORBIDDEN.value(), ex.getMessage()));
    }

    @ExceptionHandler(AppValidationException.class)
    public ResponseEntity<@NotNull ErrorResponse> handleAppValidationException(AppValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.create(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<@NotNull ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> errorDetails = new LinkedHashMap<>();
        errorDetails.put("message", "Validation failed");
        errorDetails.put("fieldErrors", fieldErrors);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(java.time.Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(errorDetails)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<@NotNull ErrorResponse> handleRateLimitExceededException(RateLimitExceededException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(java.time.Instant.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(java.util.Map.of(
                        "message", ex.getMessage(),
                        "limitType", ex.getLimitType(),
                        "limit", ex.getLimit(),
                        "used", ex.getCurrentUsage(),
                        "remaining", 0
                ))
                .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<@NotNull ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.create(HttpStatus.FORBIDDEN.value(), "Access denied"));
    }

    @ExceptionHandler(CloudStorageException.class)
    public ResponseEntity<@NotNull ErrorResponse> handleCloudStorageException(CloudStorageException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.create(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<@NotNull ErrorResponse> handleGenericException(Exception ex) {
        // Log the full exception for debugging, but don't expose details to client
        log.error("Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.create(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred"));
    }
}
