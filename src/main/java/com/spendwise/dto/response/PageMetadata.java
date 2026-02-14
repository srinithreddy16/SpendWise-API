package com.spendwise.dto.response;

/**
 * Pagination metadata for list responses.
 * It is a DTO that contains information about pagination, not the actual data.
 */
public record PageMetadata(
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        int numberOfElements
) {}
