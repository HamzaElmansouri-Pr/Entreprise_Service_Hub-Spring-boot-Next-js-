package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.Slide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SlideRepository extends JpaRepository<Slide, Long> {

    @Query("SELECT s FROM Slide s ORDER BY s.displayOrder ASC")
    List<Slide> findAllOrderByDisplayOrderAsc();
}
