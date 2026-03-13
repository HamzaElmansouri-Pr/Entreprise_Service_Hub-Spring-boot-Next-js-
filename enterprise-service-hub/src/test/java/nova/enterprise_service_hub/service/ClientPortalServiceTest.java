package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.ClientPortalDTO;
import nova.enterprise_service_hub.dto.ClientProfileUpdateDTO;
import nova.enterprise_service_hub.dto.PortalProjectDetailDTO;
import nova.enterprise_service_hub.model.ClientUser;
import nova.enterprise_service_hub.model.Project;
import nova.enterprise_service_hub.repository.ClientUserRepository;
import nova.enterprise_service_hub.repository.InvoiceRepository;
import nova.enterprise_service_hub.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientPortalServiceTest {

    @Mock
    private ClientUserRepository clientUserRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private StripeService stripeService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ClientPortalService clientPortalService;

    private ClientUser clientUser;
    private Project project;

    @BeforeEach
    void setUp() {
        clientUser = new ClientUser();
        clientUser.setId(1L);
        clientUser.setFullName("John Doe");
        clientUser.setEmail("john@example.com");
        clientUser.setTenantId("tenant-1");
        clientUser.setActive(true);
        clientUser.setPasswordHash("hashed_password");
        clientUser.setCompanyName("Acme Corp");

        project = new Project();
        project.setId(10L);
        project.setName("Website Redesign");
        project.setClientName("Acme Corp");
        project.setTenantId("tenant-1");
        project.setUpdates(new ArrayList<>());
        project.setFiles(new ArrayList<>());
    }

    @Test
    void authenticateClient_shouldReturnClient_whenCredentialsValid() {
        when(clientUserRepository.findByEmail("john@example.com")).thenReturn(Optional.of(clientUser));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);

        ClientUser result = clientPortalService.authenticateClient("john@example.com", "password123");

        assertNotNull(result);
        assertEquals("John Doe", result.getFullName());
    }

    @Test
    void getDashboard_shouldReturnProjectsAndInvoices() {
        when(clientUserRepository.findById(1L)).thenReturn(Optional.of(clientUser));
        when(projectRepository.findByClientNameIgnoreCase("Acme Corp")).thenReturn(List.of(project));
        when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc("tenant-1")).thenReturn(List.of());

        ClientPortalDTO dashboard = clientPortalService.getDashboard(1L);

        assertNotNull(dashboard);
        assertEquals("John Doe", dashboard.clientName());
        assertEquals(1, dashboard.projects().size());
        assertEquals("Website Redesign", dashboard.projects().get(0).name());
    }

    @Test
    void getProjectDetails_shouldReturnDetails_whenProjectBelongsToClient() {
        when(clientUserRepository.findById(1L)).thenReturn(Optional.of(clientUser));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        // Project belongs to Acme Corp, Client is Acme Corp

        PortalProjectDetailDTO details = clientPortalService.getProjectDetails(1L, 10L);

        assertNotNull(details);
        assertEquals("Website Redesign", details.name());
        assertEquals(0, details.updates().size());
    }

    @Test
    void getProjectDetails_shouldThrowException_whenProjectDoesNotMatchClient() {
        when(clientUserRepository.findById(1L)).thenReturn(Optional.of(clientUser));
        Project otherProject = new Project();
        otherProject.setId(11L);
        otherProject.setName("Secret Project");
        otherProject.setClientName("Other Corp");
        otherProject.setTenantId("tenant-1"); // Allow tenant match but client name mismatch
        when(projectRepository.findById(11L)).thenReturn(Optional.of(otherProject));

        assertThrows(SecurityException.class, () -> clientPortalService.getProjectDetails(1L, 11L));
    }

    @Test
    void updateClientProfile_shouldUpdateAllowedFields() {
        when(clientUserRepository.findById(1L)).thenReturn(Optional.of(clientUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("hashed_newPassword");

        ClientProfileUpdateDTO updateDTO = new ClientProfileUpdateDTO("Jane Doe", "newPassword", "+15551234567");

        clientPortalService.updateClientProfile(1L, updateDTO);

        verify(clientUserRepository).save(clientUser);
        assertEquals("Jane Doe", clientUser.getFullName());
        assertEquals("+15551234567", clientUser.getPhoneNumber());
        assertEquals("hashed_newPassword", clientUser.getPasswordHash());
    }
}
