package com.agribid.nexus.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Base identity entity for every actor in the system.
 *
 * InheritanceType.JOINED is used deliberately over SINGLE_TABLE:
 * it keeps each role's schema normalized (no dozens of nullable
 * role-specific columns on a shared table) while still allowing
 * polymorphic queries and a single unified identity table for
 * authentication purposes.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "kyc_verified", nullable = false)
    private boolean kycVerified = false;

    /**
     * Optional, self-linked by the user via
     * WhatsAppController.linkPhoneNumber() — never trusted as an
     * identity claim by itself, only used to route an already-
     * authenticated action's follow-up messages to the right
     * account. Unique so one number can't silently impersonate
     * multiple accounts.
     */
    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * BCP-47-ish language code (e.g. "en", "hi", "ta", "te", "mr",
     * "pa", "gu", "kn", "bn", "ml", "or") the user wants AI-generated
     * text back in — crop grading notes, reserve-price reasoning,
     * demand forecasts, negotiation replies. Defaults to "en" so
     * every existing/omitted registration keeps working unchanged.
     * See ai/util/LanguageInstructions for how this is turned into a
     * model instruction.
     */
    @Column(name = "preferred_language", nullable = false)
    private String preferredLanguage = "en";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected User(String email, String passwordHash, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }
}