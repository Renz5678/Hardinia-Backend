package org.example.flowerapp.Exceptions.EntityNotFoundExceptions;

public class FlowerNotFoundException extends RuntimeException{
    public FlowerNotFoundException(String message) {
        super(message);
    }

    public FlowerNotFoundException(long flower_id) {
        super("Flower with id " + flower_id + " not found");
    }
}
