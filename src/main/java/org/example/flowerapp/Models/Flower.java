package org.example.flowerapp.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.flowerapp.Models.Enums.FlowerColor;

import java.time.LocalDateTime;
import java.util.List;


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

    @Column(name="max_height")
    private Double maxHeight;

    @Column(name="growth_rate")
    private Double growthRate;

    @Column(name="auto_scheduling")
    private boolean autoScheduling = true;

    // One-to-Many relationship with Growth
    @OneToMany(mappedBy = "flower", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Growth> growthRecords;

    /**
     * Gets the most recent growth record for this flower
     * @return the latest Growth record, or null if none exists
     */
    @Transient
    public Growth getLatestGrowth() {
        if (growthRecords == null || growthRecords.isEmpty()) {
            return null;
        }

        return growthRecords.stream()
                .max((g1, g2) -> g1.getRecordedAt().compareTo(g2.getRecordedAt()))
                .orElse(null);
    }
}