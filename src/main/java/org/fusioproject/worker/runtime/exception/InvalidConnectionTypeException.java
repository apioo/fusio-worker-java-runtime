package org.fusioproject.worker.runtime.exception;

public class InvalidConnectionTypeException extends RuntimeException
{
    public InvalidConnectionTypeException(String message) {
        super(message);
    }

    public InvalidConnectionTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
