package com.agribid.nexus.service.impl;

import com.agribid.nexus.domain.user.DistributorProfile;
import com.agribid.nexus.domain.user.FarmerProfile;
import com.agribid.nexus.domain.user.User;
import com.agribid.nexus.dto.request.AuthRequest;
import com.agribid.nexus.dto.request.RegisterRequest;
import com.agribid.nexus.dto.response.AuthResponse;
import com.agribid.nexus.repository.DistributorProfileRepository;
import com.agribid.nexus.repository.FarmerProfileRepository;
import com.agribid.nexus.repository.UserRepository;
import com.agribid.nexus.security.JwtTokenProvider;
import com.agribid.nexus.security.UserPrincipal;
import com.agribid.nexus.service.AuthService;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final FarmerProfileRepository farmerProfileRepository;
    private final DistributorProfileRepository distributorProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    /**
     * kycVerified defaults to false on every new account regardless
     * of role — verification is a deliberate, separate admin action
     * (not modeled here), which is precisely what lets the
     * KycAuthorizationManager filter-chain gate mean something: an
     * account existing is not the same as an account being trusted
     * to bid.
     */
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EntityExistsException("An account already exists for " + request.email());
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        User savedUser = switch (request.role()) {
            case FARMER -> farmerProfileRepository.save(
                new FarmerProfile(request.email(), hashedPassword, request.district(), request.state()));
            case DISTRIBUTOR -> distributorProfileRepository.save(
                new DistributorProfile(request.email(), hashedPassword, request.businessLicenseNumber(), request.warehouseRegion()));
            case AGRONOMIST, ADMIN -> throw new UnsupportedOperationException(
                "Self-registration is not permitted for role " + request.role() + " — provision via an admin endpoint");
        };

        UserPrincipal principal = new UserPrincipal(savedUser);
        String token = jwtTokenProvider.generateToken(principal);

        return new AuthResponse(token, savedUser.getId(), savedUser.getEmail(), savedUser.getRole(), savedUser.isKycVerified());
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            String token = jwtTokenProvider.generateToken(principal);

            return new AuthResponse(token, principal.getId(), principal.getEmail(), principal.getRole(), principal.isKycVerified());
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }
}
