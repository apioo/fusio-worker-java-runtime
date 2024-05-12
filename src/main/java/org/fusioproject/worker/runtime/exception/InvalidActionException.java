package org.fusioproject.worker.runtime.exception;

public class InvalidActionException extends RuntimeException
{
    public InvalidActionException(String message) {
        super(message);
    }

    public InvalidActionException(String message, Throwable cause) {
        super(message, cause);
    }
}
