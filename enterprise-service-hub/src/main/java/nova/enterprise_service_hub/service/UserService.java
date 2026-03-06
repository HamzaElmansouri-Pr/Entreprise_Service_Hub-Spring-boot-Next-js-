package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.UserCreateRequest;
import nova.enterprise_service_hub.dto.UserDTO;
import nova.enterprise_service_hub.dto.UserPatchRequest;
import nova.enterprise_service_hub.exception.ResourceNotFoundException;
import nova.enterprise_service_hub.model.Role;
import nova.enterprise_service_hub.model.User;
import nova.enterprise_service_hub.repository.RoleRepository;
import nova.enterprise_service_hub.repository.UserRepository;
import nova.enterprise_service_hub.security.TenantContext;
import nova.enterprise_service_hub.util.StringSanitizer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDTO createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        Role userRole = roleRepository.findByName(ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + ROLE_USER));

        User user = new User();
        user.setFullName(StringSanitizer.stripAll(request.fullName()));
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setTenantId(TenantContext.getTenantId()); // Inherit tenant from the creating admin
        user.setEnabled(true);
        user.getRoles().add(userRole);

        if (ROLE_ADMIN.equalsIgnoreCase(request.role())) {
            Role adminRole = roleRepository.findByName(ROLE_ADMIN)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + ROLE_ADMIN));
            user.getRoles().add(adminRole);
        }

        return convertToDTO(userRepository.save(user));
    }

    @Transactional
    public UserDTO patchUser(Long userId, UserPatchRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (request.fullName() != null) {
            user.setFullName(StringSanitizer.stripAll(request.fullName()));
        }
        if (request.email() != null) {
            String normalizedEmail = request.email().trim().toLowerCase();
            if (!normalizedEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(normalizedEmail)) {
                throw new IllegalArgumentException("Email already registered: " + normalizedEmail);
            }
            user.setEmail(normalizedEmail);
        }
        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.enabled() != null) {
            if (!request.enabled() && hasRole(user, ROLE_ADMIN) && userRepository.countByRoles_NameAndEnabledTrue(ROLE_ADMIN) <= 1) {
                throw new IllegalArgumentException("Cannot disable the last enabled admin user");
            }
            user.setEnabled(request.enabled());
        }

        return convertToDTO(userRepository.save(user));
    }

    @Transactional
    public UserDTO updateUserRole(Long userId, String roleName, boolean assign) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        if (assign) {
            user.getRoles().add(role);
        } else {
            if (ROLE_ADMIN.equals(role.getName()) && hasRole(user, ROLE_ADMIN)
                    && userRepository.countByRoles_NameAndEnabledTrue(ROLE_ADMIN) <= 1) {
                throw new IllegalArgumentException("Cannot remove ROLE_ADMIN from the last enabled admin user");
            }
            user.getRoles().remove(role);
        }

        User updated = userRepository.save(user);
        return convertToDTO(updated);
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> roleName.equals(role.getName()));
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setTenantId(user.getTenantId());
        dto.setEnabled(user.isEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setRoles(user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet()));
        return dto;
    }
}
