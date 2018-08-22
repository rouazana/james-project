package org.apache.james.blob.api;

public class ObjectStoreException extends RuntimeException {

    public ObjectStoreException(String message) {
        super(message);
    }

    public ObjectStoreException(String message, Throwable cause) {
        super(message,cause);
    }
}
