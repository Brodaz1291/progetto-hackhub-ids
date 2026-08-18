package it.unicam.cs.hackhub.application.exceptions;

/**
 * Thrown when the data supplied by the client does not pass a domain check.
 *
 * Corresponds to HTTP 400 Bad Request.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
