package com.agribid.nexus.domain.logistics;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String region;

    @Column(name = "capacity_kg", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal capacityKg;

    @Column(name = "current_occupied_kg", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal currentOccupiedKg = java.math.BigDecimal.ZERO;

    public Warehouse(String name, String region, java.math.BigDecimal capacityKg) {
        this.name = name;
        this.region = region;
        this.capacityKg = capacityKg;
    }

    public java.math.BigDecimal availableCapacityKg() {
        return capacityKg.subtract(currentOccupiedKg);
    }
}