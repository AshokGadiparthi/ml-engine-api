package com.mlengine.model.entity;

import com.mlengine.model.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Project entity - represents an ML project.
 * Maps to your React UI's project concept (e.g., "Customer Churn Prediction").
 */
@Entity
@Table(name = "projects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "color")
    private String color;  // For UI theming

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @Column(name = "owner_id")
    private String ownerId;

    @Column(name = "owner_email")
    private String ownerEmail;

    @Column(name = "team_members")
    @Builder.Default
    private Integer teamMembers = 1;

    // Statistics (updated periodically)
    @Column(name = "models_count")
    @Builder.Default
    private Integer modelsCount = 0;

    @Column(name = "deployed_models_count")
    @Builder.Default
    private Integer deployedModelsCount = 0;

    @Column(name = "datasets_count")
    @Builder.Default
    private Integer datasetsCount = 0;

    @Column(name = "data_sources_count")
    @Builder.Default
    private Integer dataSourcesCount = 0;

    @Column(name = "total_data_size_bytes")
    @Builder.Default
    private Long totalDataSizeBytes = 0L;

    @Column(name = "avg_accuracy")
    private Double avgAccuracy;

    @Column(name = "accuracy_trend")
    private Double accuracyTrend;  // % change from last week

    @Column(name = "predictions_count")
    @Builder.Default
    private Long predictionsCount = 0L;

    @Column(name = "predictions_this_month")
    @Builder.Default
    private Long predictionsThisMonth = 0L;

    // Relationships
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Dataset> datasets = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DataSource> dataSources = new ArrayList<>();

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
