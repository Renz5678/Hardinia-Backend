package org.example.flowerapp.Exceptions.EntityNotFoundExceptions;

public class GrowthNotFoundException extends RuntimeException{
    public GrowthNotFoundException(String message) {
        super(message);
    }

    public GrowthNotFoundException(long growth_id) {
        super("Growth with id " + growth_id + " not found");
    }
}
