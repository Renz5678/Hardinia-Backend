package org.example.flowerapp.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.flowerapp.Models.Enums.MaintenanceType;

import java.time.LocalDateTime;

@Entity
@Table(name="maintenance")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Maintenance {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="task_id")
    private long task_id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="flower_id", nullable=false)
    private Flower flower;

    @Enumerated(EnumType.STRING)
    @Column(name="maintenance_type")
    private MaintenanceType taskType;

    @Column(name="maintenance_date")
    private LocalDateTime scheduledDate;

    @Column(name="notes")
    private String notes;

    @Column(name="performed_by")
    private String performedBy;

    @Column(name="created_at")
    private LocalDateTime createdAt;
}
