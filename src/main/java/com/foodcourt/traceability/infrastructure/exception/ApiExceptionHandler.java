package com.foodcourt.traceability.infrastructure.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.foodcourt.traceability.domain.exception.AccessDenied;
import com.foodcourt.traceability.domain.exception.AccountDirectoryUnavailable;
import com.foodcourt.traceability.domain.exception.AccountNotFound;
import com.foodcourt.traceability.domain.exception.AuthenticationRequired;
import com.foodcourt.traceability.domain.exception.IllegalStatusTransition;
import com.foodcourt.traceability.domain.exception.InvalidTraceRequest;
import com.foodcourt.traceability.domain.exception.OrderTraceNotFound;
import com.foodcourt.traceability.domain.exception.TraceConflict;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Maps domain failures to {@code application/problem+json}.
 * Bean Validation is left to Spring, which already returns HTTP 400.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AuthenticationRequired.class)
    ProblemDetail authenticationRequired(AuthenticationRequired exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", exception, request);
    }

    @ExceptionHandler(AccessDenied.class)
    ProblemDetail accessDenied(AccessDenied exception, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", exception, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail invalidConstraint(ConstraintViolationException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception, request);
    }

    @ExceptionHandler(InvalidTraceRequest.class)
    ProblemDetail invalidTrace(InvalidTraceRequest exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid trace", exception, request);
    }

    @ExceptionHandler(OrderTraceNotFound.class)
    ProblemDetail missingTrace(OrderTraceNotFound exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Order trace not found", exception, request);
    }

    @ExceptionHandler(AccountNotFound.class)
    ProblemDetail missingAccount(AccountNotFound exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Account not found", exception, request);
    }

    @ExceptionHandler(IllegalStatusTransition.class)
    ProblemDetail illegalTransition(IllegalStatusTransition exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Status change not allowed", exception, request);
    }

    @ExceptionHandler(TraceConflict.class)
    ProblemDetail traceConflict(TraceConflict exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Status already recorded", exception, request);
    }

    @ExceptionHandler(AccountDirectoryUnavailable.class)
    ProblemDetail directoryDown(AccountDirectoryUnavailable exception, HttpServletRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "User service unavailable", exception, request);
    }

    private static ProblemDetail problem(
            HttpStatus status,
            String title,
            RuntimeException exception,
            HttpServletRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        body.setTitle(title);
        body.setInstance(URI.create(request.getRequestURI()));
        return body;
    }
}
