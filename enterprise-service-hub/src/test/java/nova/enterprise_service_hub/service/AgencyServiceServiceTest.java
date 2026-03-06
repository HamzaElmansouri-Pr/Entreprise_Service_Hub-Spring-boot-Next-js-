package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.AgencyServiceDTO;
import nova.enterprise_service_hub.dto.ServiceRequest;
import nova.enterprise_service_hub.mapper.AgencyServiceMapper;
import nova.enterprise_service_hub.model.AgencyService;
import nova.enterprise_service_hub.repository.AgencyServiceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AgencyServiceService.
 */
@ExtendWith(MockitoExtension.class)
class AgencyServiceServiceTest {

    @Mock private AgencyServiceRepository repository;
    @Mock private AgencyServiceMapper mapper;
    @Mock private AssetService assetService;

    private AgencyServiceService service;

    @BeforeEach
    void setUp() {
        service = new AgencyServiceService(repository, mapper, assetService);
    }

    private AgencyService createService(Long id, String title, String slug) {
        AgencyService s = new AgencyService();
        s.setId(id);
        s.setTitle(title);
        s.setSlug(slug);
        s.setActive(true);
        return s;
    }

    @Nested
    @DisplayName("findAllActive")
    class FindAllActive {

        @Test
        @DisplayName("should return only active services")
        void returnsActive() {
            AgencyService s1 = createService(1L, "Web Dev", "web-dev");
            AgencyServiceDTO dto1 = new AgencyServiceDTO(1L, "Web Dev", "web-dev",
                    null, null, null, 0, true, null, null);

            when(repository.findAllByActiveTrueOrderByDisplayOrderAscCreatedAtDesc())
                    .thenReturn(List.of(s1));
            when(mapper.toDtoList(anyList())).thenReturn(List.of(dto1));

            List<AgencyServiceDTO> result = service.findAllActive();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).title()).isEqualTo("Web Dev");
        }
    }

    @Nested
    @DisplayName("findBySlug")
    class FindBySlug {

        @Test
        @DisplayName("should return service by slug")
        void returnsBySlug() {
            AgencyService s = createService(1L, "Cloud", "cloud-services");
            AgencyServiceDTO dto = new AgencyServiceDTO(1L, "Cloud", "cloud-services",
                    null, null, null, 0, true, null, null);

            when(repository.findBySlugAndActiveTrue("cloud-services")).thenReturn(Optional.of(s));
            when(mapper.toDto(s)).thenReturn(dto);

            var result = service.findBySlug("cloud-services");

            assertThat(result).isPresent();
            assertThat(result.get().slug()).isEqualTo("cloud-services");
        }

        @Test
        @DisplayName("should return empty when slug not found")
        void returnsEmpty() {
            when(repository.findBySlugAndActiveTrue("unknown")).thenReturn(Optional.empty());

            var result = service.findBySlug("unknown");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should hard delete existing service")
        void deletesService() {
            when(repository.existsById(1L)).thenReturn(true);

            service.delete(1L);

            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("should throw when service not found")
        void throwsWhenNotFound() {
            when(repository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> service.delete(999L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }
}
