package org.example.flowerapp.Exceptions.EntityNotFoundExceptions;

public class MaintenanceNotFoundException extends RuntimeException{
    public MaintenanceNotFoundException(String message) {
        super(message);
    }

    public MaintenanceNotFoundException(long maintenance_id) {
        super("Task with id " + maintenance_id + " not found");
    }

}
