package org.fusioproject.worker.runtime.exception;

public class ConnectionNotFoundException extends RuntimeException
{
    public ConnectionNotFoundException(String message) {
        super(message);
    }

    public ConnectionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
