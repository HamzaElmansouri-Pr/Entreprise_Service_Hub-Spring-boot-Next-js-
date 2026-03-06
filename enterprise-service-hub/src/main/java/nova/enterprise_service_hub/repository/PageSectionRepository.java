package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.PageSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PageSectionRepository extends JpaRepository<PageSection, Long> {
    List<PageSection> findAllByPageNameIgnoreCaseOrderByDisplayOrderAsc(String pageName);
}
