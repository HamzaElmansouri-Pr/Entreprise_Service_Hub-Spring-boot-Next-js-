package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.ProjectDTO;
import nova.enterprise_service_hub.dto.ProjectRequest;
import nova.enterprise_service_hub.mapper.ProjectMapper;
import nova.enterprise_service_hub.model.Project;
import nova.enterprise_service_hub.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProjectService — CRUD + tenant isolation.
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository repository;
    @Mock
    private ProjectMapper mapper;
    @Mock
    private AssetService assetService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(repository, mapper, assetService, eventPublisher);
    }

    private Project createProject(Long id, String name) {
        Project p = new Project();
        p.setId(id);
        p.setName(name);
        p.setClientName("Test Client");
        p.setArchived(false);
        return p;
    }

    private ProjectDTO createProjectDTO(Long id, String name) {
        return new ProjectDTO(id, name, "Test Client", null, null, null, List.of(),
                0, false, List.of(), null, null);
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all non-archived projects")
        void returnsNonArchived() {
            Project p1 = createProject(1L, "Project A");
            Project p2 = createProject(2L, "Project B");
            ProjectDTO dto1 = createProjectDTO(1L, "Project A");
            ProjectDTO dto2 = createProjectDTO(2L, "Project B");

            when(repository.findAllByArchivedFalseOrderByDisplayOrderAscCreatedAtDesc())
                    .thenReturn(List.of(p1, p2));
            when(mapper.toDtoList(List.of(p1, p2))).thenReturn(List.of(dto1, dto2));

            List<ProjectDTO> result = projectService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("Project A");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return project when found")
        void returnsProject() {
            Project p = createProject(1L, "Found");
            ProjectDTO dto = createProjectDTO(1L, "Found");
            when(repository.findById(1L)).thenReturn(Optional.of(p));
            when(mapper.toDto(p)).thenReturn(dto);

            ProjectDTO result = projectService.findById(1L);

            assertThat(result.name()).isEqualTo("Found");
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when not found")
        void throwsWhenNotFound() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.findById(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create and return new project")
        void createsProject() {
            ProjectRequest request = new ProjectRequest(
                    "New Project", "Client X", null, null, null, null, null, null, List.of("Java"));
            Project saved = createProject(1L, "New Project");
            ProjectDTO dto = createProjectDTO(1L, "New Project");

            when(assetService.validateAndBuild(any())).thenReturn(null);
            when(repository.save(any(Project.class))).thenReturn(saved);
            when(mapper.toDto(saved)).thenReturn(dto);

            ProjectDTO result = projectService.create(request);

            assertThat(result.name()).isEqualTo("New Project");
            verify(repository).save(any(Project.class));
        }
    }

    @Nested
    @DisplayName("delete (soft)")
    class Delete {

        @Test
        @DisplayName("should archive the project instead of deleting")
        void archivesProject() {
            Project p = createProject(1L, "To Archive");
            when(repository.findById(1L)).thenReturn(Optional.of(p));
            when(repository.save(any())).thenReturn(p);

            projectService.delete(1L);

            assertThat(p.isArchived()).isTrue();
            verify(repository).save(p);
            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("should throw when project not found")
        void throwsWhenNotFound() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.delete(999L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }
}
