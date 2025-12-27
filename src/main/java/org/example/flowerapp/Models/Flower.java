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

    @Column(name="grid_position")
    private Integer gridPosition;

    @Column(name="water_frequency_days")
    private Integer waterFrequencyDays;

    @Column(name="fertilize_frequency_days")
    private Integer fertilizeFrequencyDays;

    @Column(name="prune_frequency_days")
    private Integer pruneFrequencyDays;

    @Column(name="last_watered")
    private LocalDateTime lastWateredDate;

    @Column(name="last_fertilized")
    private LocalDateTime lastFertilizedDate;

    @Column(name="last_pruned_date")
    private LocalDateTime lastPrunedDate;

    @Column(name="auto_scheduling")
    private boolean autoScheduling = true;
}
