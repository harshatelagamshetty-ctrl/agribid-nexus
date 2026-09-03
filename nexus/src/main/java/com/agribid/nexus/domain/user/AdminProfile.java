package com.agribid.nexus.domain.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Deliberately minimal — admins have no extra profile fields beyond
 * what User already provides. This class exists purely so the JOINED
 * inheritance hierarchy has a concrete subclass for the ADMIN
 * discriminator value; without it, Hibernate cannot instantiate a
 * User row with user_type = 'ADMIN' at all, which is exactly the bug
 * this fixes (see admin_profiles table added in V6 migration).
 */
@Entity
@Table(name = "admin_profiles")
@DiscriminatorValue("ADMIN")
@Getter
@Setter
@NoArgsConstructor
public class AdminProfile extends User {

    public AdminProfile(String email, String passwordHash) {
        super(email, passwordHash, Role.ADMIN);
    }
}