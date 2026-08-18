package it.unicam.cs.hackhub.application.exceptions;

/**
 * Thrown when the credentials supplied on login are not valid.
 *
 * Corresponds to HTTP 401 Unauthorized: it is kept distinct from ValidationException so that
 * a client can tell a wrong password from a malformed request.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}
