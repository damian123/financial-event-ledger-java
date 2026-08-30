package io.github.damian123.eventledger.api.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApi(ApiException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        detail.setTitle(ex.getCode());
        detail.setProperty("code", ex.getCode());
        detail.setProperty("timestamp", Instant.now().toString());
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Missing required header: " + ex.getHeaderName()
        );
        detail.setTitle("MISSING_HEADER");
        detail.setProperty("code", "MISSING_HEADER");
        detail.setProperty("timestamp", Instant.now().toString());
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        detail.setTitle("VALIDATION_ERROR");
        detail.setProperty("code", "VALIDATION_ERROR");
        detail.setProperty("timestamp", Instant.now().toString());
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setTitle("VALIDATION_ERROR");
        detail.setProperty("code", "VALIDATION_ERROR");
        detail.setProperty("timestamp", Instant.now().toString());
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request body is invalid JSON");
        detail.setTitle("INVALID_JSON");
        detail.setProperty("code", "INVALID_JSON");
        detail.setProperty("timestamp", Instant.now().toString());
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }
}
