package nova.enterprise_service_hub.dto;

import java.util.List;

/**
 * Generic paginated response wrapper.
 * <p>
 * Wraps any list of DTOs with pagination metadata for consistent
 * API responses across all list endpoints.
 *
 * @param <T> the DTO type contained in the page
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {

    /**
     * Factory method to create a PageResponse from a Spring Data Page.
     */
    public static <T> PageResponse<T> from(org.springframework.data.domain.Page<T> springPage) {
        return new PageResponse<>(
                springPage.getContent(),
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isLast());
    }
}
