package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.dto.NotificationDTO;
import nova.enterprise_service_hub.dto.PageResponse;
import nova.enterprise_service_hub.model.Notification;
import nova.enterprise_service_hub.model.Notification.NotificationType;
import nova.enterprise_service_hub.repository.NotificationRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Notification Service — Manages in-app notifications with SSE real-time push.
 * <p>
 * Provides CRUD operations for notifications and a Server-Sent Events (SSE)
 * endpoint for real-time push to connected clients.
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository repository;
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    // ── Queries ──────────────────────────────────────────────────────────

    public PageResponse<NotificationDTO> getNotifications(Long userId, Pageable pageable) {
        return PageResponse.from(repository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toDto));
    }

    public List<NotificationDTO> getUnread(Long userId) {
        return repository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).toList();
    }

    public long getUnreadCount(Long userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    // ── Commands ─────────────────────────────────────────────────────────

    @Transactional
    public NotificationDTO create(String tenantId, Long userId, String title, String message, NotificationType type) {
        Notification notification = new Notification(tenantId, userId, title, message, type);
        notification = repository.save(notification);

        // Push via SSE to connected clients
        NotificationDTO dto = toDto(notification);
        pushToUser(userId, dto);
        return dto;
    }

    @Transactional
    public NotificationDTO create(String tenantId, Long userId, String title, String message,
            NotificationType type, String entityType, Long entityId) {
        Notification notification = new Notification(tenantId, userId, title, message, type);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notification = repository.save(notification);

        NotificationDTO dto = toDto(notification);
        pushToUser(userId, dto);
        return dto;
    }

    @Transactional
    public void markRead(Long notificationId) {
        repository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            repository.save(n);
        });
    }

    @Transactional
    public void markAllRead(Long userId) {
        repository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .forEach(n -> {
                    n.setRead(true);
                    repository.save(n);
                });
    }

    // ── SSE Streaming ────────────────────────────────────────────────────

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 min timeout
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        return emitter;
    }

    private void pushToUser(Long userId, NotificationDTO dto) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null)
            return;

        userEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("notification").data(dto));
            } catch (IOException e) {
                removeEmitter(userId, emitter);
            }
        });
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    // ── Mapping ──────────────────────────────────────────────────────────

    private NotificationDTO toDto(Notification n) {
        return new NotificationDTO(
                n.getId(), n.getTitle(), n.getMessage(),
                n.getType().name(), n.isRead(),
                n.getEntityType(), n.getEntityId(), n.getCreatedAt());
    }
}
