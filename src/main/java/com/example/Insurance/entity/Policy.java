package com.example.Insurance.entity;

import com.example.Insurance.Enums.PolicyStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String icon;

    @Column(nullable = false, length = 1000)
    private String description;

    // FIXED: Changed from LAZY to EAGER to prevent lazy loading error
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "policy_benefits", joinColumns = @JoinColumn(name = "policy_id"))
    @Column(name = "benefit")
    private List<String> benefits;

    @Column(nullable = false)
    private Double premiumAmount;

    @Column(nullable = false)
    private Double coverageAmount;

    @Column(nullable = false)
    private String vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyStatus status;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd") // Added for proper JSON serialization
    private LocalDate createdDate;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd") // Added for proper JSON serialization
    private LocalDate updatedDate;

    // Added PrePersist and PreUpdate methods for automatic date handling
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDate.now();
        }
        if (updatedDate == null) {
            updatedDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDate.now();
    }
}
