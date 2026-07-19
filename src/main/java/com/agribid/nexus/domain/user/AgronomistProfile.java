package com.agribid.nexus.domain.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "agronomist_profiles")
@DiscriminatorValue("AGRONOMIST")
@Getter
@Setter
@NoArgsConstructor
public class AgronomistProfile extends User {

    @Column(name = "certification_number", nullable = false, unique = true)
    private String certificationNumber;

    @Column(name = "specialization")
    private String specialization;

    public AgronomistProfile(String email, String passwordHash, String certificationNumber, String specialization) {
        super(email, passwordHash, Role.AGRONOMIST);
        this.certificationNumber = certificationNumber;
        this.specialization = specialization;
    }
}