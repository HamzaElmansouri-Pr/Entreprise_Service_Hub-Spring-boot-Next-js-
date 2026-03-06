package nova.enterprise_service_hub.controller;

import nova.enterprise_service_hub.dto.NotificationDTO;
import nova.enterprise_service_hub.dto.PageResponse;
import nova.enterprise_service_hub.model.User;
import nova.enterprise_service_hub.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * REST + SSE Controller for Notifications.
 * <p>
 * Provides paginated notification list, unread count,
 * mark-read actions, and a real-time SSE event stream.
 */
@RestController
@RequestMapping("/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<NotificationDTO>> getNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getNotifications(user.getId(), PageRequest.of(page, size)));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDTO>> getUnread(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getUnread(user.getId()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(user.getId())));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal User user) {
        notificationService.markAllRead(user.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * SSE endpoint — clients connect here for real-time notification push.
     * Connection stays open for 5 minutes, then auto-disconnects.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal User user) {
        return notificationService.subscribe(user.getId());
    }
}
