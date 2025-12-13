package org.example.flowerapp.Controller;

import org.example.flowerapp.Exceptions.BusinessLogicExceptions.DuplicateFlowerException;
import org.example.flowerapp.Exceptions.BusinessLogicExceptions.FlowerHasDependenciesException;
import org.example.flowerapp.Exceptions.DatabaseOperationExceptions.DatabaseOperationException;
import org.example.flowerapp.Exceptions.EntityNotFoundExceptions.FlowerNotFoundException;
import org.example.flowerapp.Exceptions.EntityNotFoundExceptions.GrowthNotFoundException;
import org.example.flowerapp.Exceptions.EntityNotFoundExceptions.MaintenanceNotFoundException;
import org.example.flowerapp.Exceptions.ValidationExceptions.InvalidFlowerDataException;
import org.example.flowerapp.Exceptions.ValidationExceptions.InvalidGrowthDataException;
import org.example.flowerapp.Exceptions.ValidationExceptions.InvalidMaintenanceDataException;
import org.example.flowerapp.Models.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class ExceptionController {

    @ExceptionHandler({
            FlowerNotFoundException.class,
            MaintenanceNotFoundException.class,
            GrowthNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFoundException(RuntimeException e) {
        return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({
            InvalidFlowerDataException.class,
            InvalidMaintenanceDataException.class,
            InvalidGrowthDataException.class
    })
    public ResponseEntity<ErrorResponse> handleValidationException(RuntimeException e) {
        return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
            DuplicateFlowerException.class,
            FlowerHasDependenciesException.class
    })
    public ResponseEntity<ErrorResponse> handleConflictException(RuntimeException e) {
        return buildErrorResponse(e.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DatabaseOperationException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseException(DatabaseOperationException e) {
        return buildErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(String message, HttpStatus status) {
        ErrorResponse error = new ErrorResponse(
                status.value(),
                message,
                LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(error);
    }
}