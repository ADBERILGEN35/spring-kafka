package com.startupheroes.app.exception;

public class KafkaSerializationException extends RuntimeException {

    public KafkaSerializationException(Long packageId, Throwable cause) {
        super("Failed to serialize package: " + packageId, cause);
    }
}