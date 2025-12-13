package org.example.flowerapp.Models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private int status;

    @NonNull
    private String message;

    @NonNull
    private LocalDateTime timestamp;
}
