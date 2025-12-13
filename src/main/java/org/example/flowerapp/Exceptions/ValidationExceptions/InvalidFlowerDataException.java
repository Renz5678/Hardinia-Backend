package org.example.flowerapp.Exceptions.ValidationExceptions;

public class InvalidFlowerDataException extends RuntimeException{
    public InvalidFlowerDataException(String message) {
        super(message);
    }
}
