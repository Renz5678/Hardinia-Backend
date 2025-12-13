package org.example.flowerapp.Exceptions.ValidationExceptions;

public class InvalidGrowthDataException extends RuntimeException{
    public InvalidGrowthDataException(String message) {
        super(message);
    }
}
