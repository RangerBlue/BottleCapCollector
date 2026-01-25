package com.km.bottlecapcollector.api.handler;

import com.km.bottlecapcollector.api.handler.exception.AppBadRequestException;
import com.km.bottlecapcollector.api.handler.exception.AppForbiddenException;
import com.km.bottlecapcollector.api.handler.exception.AppResourceNotFoundException;
import com.km.bottlecapcollector.api.handler.exception.AppValidationException;
import com.km.bottlecapcollector.api.handler.exception.RateLimitExceededException;
import com.km.bottlecapcollector.api.model.response.ErrorResponse;
import com.km.bottlecapcollector.api.model.response.RateLimitInfo;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

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
}
