package com.startupheroes.app.exception;

public class PackageCancelledException extends RuntimeException {

    public PackageCancelledException(Long id) {
        super("Package is cancelled and cannot be sent to Kafka. Id: " + id);
    }
}
