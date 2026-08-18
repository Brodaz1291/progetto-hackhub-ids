package it.unicam.cs.hackhub.application.exceptions;

/**
 * Thrown when the request is well formed but clashes with the current state of a resource,
 * for example a prize that has already been paid or a team that is already registered.
 *
 * Corresponds to HTTP 409 Conflict.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
