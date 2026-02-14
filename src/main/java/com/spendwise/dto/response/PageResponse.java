package com.spendwise.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic paginated response with content and metadata.
 * When you use pagination, frontend needs:
 The actual records (expenses, budgets, users)
 Pagination details (page number, total pages, etc.)
 PageResponse<T> packages both together cleanly.
 */
public record PageResponse<T>(
        List<T> content,
        PageMetadata page
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        PageMetadata metadata = new PageMetadata(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.getNumberOfElements()
        );
        return new PageResponse<>(page.getContent(), metadata);
    }
}
