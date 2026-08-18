package it.unicam.cs.hackhub.application.exceptions;

/**
 * Thrown when a resource requested by the client does not exist.
 *
 * Corresponds to HTTP 404 Not Found.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
