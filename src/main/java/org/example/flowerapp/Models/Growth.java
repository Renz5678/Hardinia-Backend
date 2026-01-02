package org.example.flowerapp.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.flowerapp.Models.Enums.GrowthStage;

import java.time.LocalDateTime;

@Entity
@Table(name="growthdetails")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Growth {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="growth_id")
    private long growth_id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="flower_id", nullable=false)
    private Flower flower;

    @Enumerated(EnumType.STRING)
    @Column(name="stage", nullable=false)
    private GrowthStage stage;

    @Column(name="height", nullable=false)
    private double height;

    @Column(name="color_changes")
    private boolean colorChanges;

    @Column(name="notes")
    private String notes;

    @Column(name="recorded_at", nullable=false)
    private LocalDateTime recordedAt;

    @Column(name="growth_since_last")
    private Double growthSinceLast; // How much the flower grew since the previous record

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }
}