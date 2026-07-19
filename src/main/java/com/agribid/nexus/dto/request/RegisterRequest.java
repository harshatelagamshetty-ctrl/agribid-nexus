package com.agribid.nexus.dto.request;

import com.agribid.nexus.domain.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Covers signup for both ROLE_FARMER and ROLE_DISTRIBUTOR. The
 * role-specific fields (district/state for farmers,
 * businessLicenseNumber/warehouseRegion for distributors) are
 * optional here and validated for presence in AuthServiceImpl based
 * on the requested role, rather than splitting into two DTOs that
 * would duplicate the shared email/password/role fields.
 */
public record RegisterRequest(

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, message = "password must be at least 8 characters")
        String password,

        @NotNull(message = "role is required")
        Role role,

        // ROLE_FARMER fields
        String district,
        String state,
        String fpoAffiliation,

        // ROLE_DISTRIBUTOR fields
        String businessLicenseNumber,
        String warehouseRegion

) {
}