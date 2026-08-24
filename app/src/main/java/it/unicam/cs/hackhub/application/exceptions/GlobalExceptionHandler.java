package it.unicam.cs.hackhub.application.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Turns the exceptions raised by the services into HTTP responses, so that a client can tell
 * an invalid request from a failure of the platform. Every handler delegates to the same
 * builder: the shape of an error is decided in one place only.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException exception,
                                                          HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException exception,
                                                              HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, exception, request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException exception,
                                                         HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, exception, request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception,
                                                        HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception, request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException exception,
                                                        HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception, request);
    }

    /**
     * An inconsistent state of the stored data, such as a hackathon without an organizer, is
     * not the fault of the client: it stays a 500, but it carries the message and it is the
     * only case worth writing in the log with its stack trace.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException exception,
                                                            HttpServletRequest request) {
        log.error("Inconsistent data on {}", request.getRequestURI(), exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception, request);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalService(ExternalServiceException exception,
                                                               HttpServletRequest request) {
        log.error("External system unavailable on {}", request.getRequestURI(), exception);
        return buildResponse(HttpStatus.BAD_GATEWAY, exception, request);
    }

    /**
     * Assembles the body of an error response.
     *
     * The timestamp is the only point of the platform that reads the system time instead of
     * the injected Clock, and the exception is deliberate: the Clock models the time of the
     * DOMAIN, when a hackathon opens or a submission is uploaded, while an error is an
     * operational fact that has to be matched with the server log, which is written in real
     * time. With app.time.mode=fixed the two would disagree, and an error raised today would
     * be dated February next to a log line dated August.
     */
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status,
                                                        Exception exception,
                                                        HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
