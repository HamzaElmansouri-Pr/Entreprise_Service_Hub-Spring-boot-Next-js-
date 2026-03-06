package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.AuthResponse;
import nova.enterprise_service_hub.dto.LoginRequest;
import nova.enterprise_service_hub.dto.RegisterRequest;
import nova.enterprise_service_hub.model.Role;
import nova.enterprise_service_hub.model.User;
import nova.enterprise_service_hub.model.RefreshToken;
import nova.enterprise_service_hub.repository.RoleRepository;
import nova.enterprise_service_hub.repository.UserRepository;
import nova.enterprise_service_hub.repository.RefreshTokenRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Authentication Service — Handles user registration and login logic.
 * <p>
 * Embeds the user's {@code tenantId} in every JWT so the
 * {@link nova.enterprise_service_hub.config.JwtAuthenticationFilter}
 * can populate {@link nova.enterprise_service_hub.security.TenantContext}.
 */
@Service
public class AuthService {

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final RefreshTokenRepository refreshTokenRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;

        public AuthService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        AuthenticationManager authenticationManager) {
                this.userRepository = userRepository;
                this.roleRepository = roleRepository;
                this.refreshTokenRepository = refreshTokenRepository;
                this.passwordEncoder = passwordEncoder;
                this.jwtService = jwtService;
                this.authenticationManager = authenticationManager;
        }

        /**
         * Register a brand-new user and auto-generate a tenant UUID.
         * The tenantId is embedded in the JWT so every subsequent request
         * is scoped to that tenant.
         */
        @Transactional
        public AuthResponse register(RegisterRequest request) {
                if (userRepository.existsByEmail(request.email())) {
                        throw new IllegalArgumentException("Email already registered: " + request.email());
                }

                Role userRole = roleRepository.findByName("ROLE_USER")
                                .orElseThrow(() -> new IllegalStateException(
                                                "Default ROLE_USER not found in database"));

                User user = new User();
                user.setFullName(request.fullName());
                user.setEmail(request.email());
                user.setPassword(passwordEncoder.encode(request.password()));
                user.setTenantId(UUID.randomUUID().toString());
                user.setRoles(Set.of(userRole));

                userRepository.save(user);

                String token = jwtService.generateToken(user, user.getTenantId());
                RefreshToken refreshToken = createRefreshToken(user);

                return new AuthResponse(
                                token,
                                refreshToken.getToken(),
                                3600000L, // 1 hour explicitly
                                user.getEmail(),
                                user.getFullName(),
                                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        }

        /**
         * Authenticate an existing user and issue a JWT with their tenantId claim.
         */
        @Transactional
        public AuthResponse login(LoginRequest request) {
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

                User user = userRepository.findByEmail(request.email())
                                .orElseThrow(() -> new IllegalArgumentException("User not found"));

                String token = jwtService.generateToken(user, user.getTenantId());
                RefreshToken refreshToken = createRefreshToken(user);

                return new AuthResponse(
                                token,
                                refreshToken.getToken(),
                                3600000L,
                                user.getEmail(),
                                user.getFullName(),
                                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        }

        @Transactional
        public AuthResponse refreshToken(String tokenValue) {
                RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenValue)
                                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

                if (refreshToken.isRevoked() || refreshToken.getExpiryDate().isBefore(Instant.now())) {
                        refreshTokenRepository.delete(refreshToken);
                        throw new IllegalArgumentException(
                                        "Refresh token was expired or revoked. Please make a new sign in request");
                }

                User user = refreshToken.getUser();
                String newToken = jwtService.generateToken(user, user.getTenantId());

                return new AuthResponse(
                                newToken,
                                refreshToken.getToken(),
                                3600000L,
                                user.getEmail(),
                                user.getFullName(),
                                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        }

        private RefreshToken createRefreshToken(User user) {
                refreshTokenRepository.deleteByUserId(user.getId()); // revoke old
                RefreshToken refreshToken = new RefreshToken();
                refreshToken.setUser(user);
                refreshToken.setToken(UUID.randomUUID().toString());
                refreshToken.setExpiryDate(Instant.now().plus(7, ChronoUnit.DAYS));
                return refreshTokenRepository.save(refreshToken);
        }
}
