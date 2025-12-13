package org.example.flowerapp.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.flowerapp.Models.Enums.FlowerColor;

import java.time.LocalDateTime;


@Entity
@Table(name="flowerdetails")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Flower {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name="flower_id")
    private long flower_id;

    @Column(name="flower_name", nullable=false)
    private String flowerName;

    @Column(name="species")
    private String species;

    @Enumerated(EnumType.STRING)
    @Column(name="color")
    private FlowerColor color;

    @Column(name="planting_date")
    private LocalDateTime plantingDate;
}
