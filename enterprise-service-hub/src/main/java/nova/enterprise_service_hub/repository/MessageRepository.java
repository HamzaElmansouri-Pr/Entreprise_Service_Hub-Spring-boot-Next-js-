package nova.enterprise_service_hub.repository;

import nova.enterprise_service_hub.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByClientUserIdOrderByCreatedAtDesc(Long clientUserId, Pageable pageable);

    long countByClientUserIdAndReadFalse(Long clientUserId);
}
