package it.unicam.cs.hackhub.application.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * The body returned by the platform when a request cannot be served.
 *
 * It keeps the four fields Spring already produced on its own (timestamp, status, error and
 * path) and adds the message of the exception, which used to stay in the server log: without
 * it a client can tell that something failed, but not what.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private LocalDateTime timestamp;

    private int status;

    private String error;

    private String message;

    private String path;
}
