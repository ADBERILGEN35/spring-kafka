package com.startupheroes.app.exception;

public class PackageNotFoundException extends RuntimeException {

    public PackageNotFoundException(Long id) {
        super("Package not found with id: " + id);
    }
}
