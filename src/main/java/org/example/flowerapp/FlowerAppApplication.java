package org.example.flowerapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlowerAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowerAppApplication.class, args);
    }

}
