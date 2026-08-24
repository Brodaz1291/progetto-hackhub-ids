package it.unicam.cs.hackhub.application.exceptions;

/**
 * Thrown when the caller is known but the operation is not open to them.
 *
 * It is the distance from AuthenticationException that gives it its meaning: there the platform
 * does not know who is asking, here it knows and refuses anyway, because the role required is
 * not one the caller covers.
 *
 * Corresponds to HTTP 403 Forbidden.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
