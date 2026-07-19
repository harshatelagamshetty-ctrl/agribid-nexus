package com.agribid.nexus.domain.crop;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // e.g. "TOMATO", "WHEAT"

    @Column(nullable = false)
    private String name;

    private String description;

    public Category(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}