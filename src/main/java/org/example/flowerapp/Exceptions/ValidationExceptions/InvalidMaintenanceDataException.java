package org.example.flowerapp.Exceptions.ValidationExceptions;

public class InvalidMaintenanceDataException extends RuntimeException{
    public InvalidMaintenanceDataException(String message) {
        super(message);
    }
}
