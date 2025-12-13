package org.example.flowerapp.Exceptions.BusinessLogicExceptions;

public class DuplicateFlowerException extends RuntimeException{
    public DuplicateFlowerException(String message) {
        super(message);
    }
}
