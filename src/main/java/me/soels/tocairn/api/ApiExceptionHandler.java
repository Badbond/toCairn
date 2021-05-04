package me.soels.tocairn.api;

import me.soels.tocairn.api.dtos.ErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.ZonedDateTime;

/**
 * Exception handler to deserialize exceptions in APIs.
 */
@ControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDto> handle(ResourceNotFoundException e) {
        return handle(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDto> handle(IllegalArgumentException e) {
        return handle(HttpStatus.BAD_REQUEST, e);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDto> handle(IllegalStateException e) {
        return handle(HttpStatus.INTERNAL_SERVER_ERROR, e);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDto> handle(Throwable e) {
        return handle(HttpStatus.INTERNAL_SERVER_ERROR, e);
    }

    public ResponseEntity<ErrorResponseDto> handle(HttpStatus status, Throwable e) {
        var body = new ErrorResponseDto(e.getMessage(), ZonedDateTime.now(), e.getClass().getSimpleName());
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error("Error during HTTP call resulting in " + status.value() + " status response.", e);
        }
        return new ResponseEntity<>(body, null, status);
    }
}
