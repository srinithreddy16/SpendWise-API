package com.spendwise.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic paginated response with content and flat metadata.
 * Reusable for any DTO type. Do not expose Page<Entity>; map to DTOs before wrapping.
 * It is a generic DTO used to return paginated API responses in a flat structure.
   It combines:
   The actual data (content)
   Pagination metadata (page info)
   All in one object.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        int numberOfElements
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.getNumberOfElements()
        );
    }
}
