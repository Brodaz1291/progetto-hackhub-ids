package it.unicam.cs.hackhub.application.exceptions;

/**
 * Thrown when an external system reached through an adapter refuses or does not confirm an
 * operation.
 *
 * Corresponds to HTTP 502 Bad Gateway: the platform is working correctly, the failure comes
 * from the system it depends on.
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }
}
