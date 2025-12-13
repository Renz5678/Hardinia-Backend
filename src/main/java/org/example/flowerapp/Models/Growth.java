package org.example.flowerapp.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.flowerapp.Models.Enums.GrowthStage;

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
    @Column(name="stage")
    private GrowthStage stage;

    @Column(name="height")
    private double height;

    @Column(name="color_changes")
    private boolean colorChanges;

    @Column(name="notes")
    private String notes;
}
