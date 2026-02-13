package com.spendwise.mapper;

import com.spendwise.domain.entity.Budget;
import com.spendwise.dto.response.BudgetResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

/**
 * MapStruct mapper for Budget entity and metrics to BudgetResponse DTO.
 * No business logic; maps categories to categoryIds and applies calculated metrics.
 */
@Mapper(componentModel = "spring")
public interface BudgetMapper {

    @Mapping(target = "categoryIds", expression = "java(budget.getCategories().stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toSet()))")
    @Mapping(target = "month", expression = "java(budget.getMonth() != null ? budget.getMonth() : 0)")
    @Mapping(target = "totalSpent", source = "totalSpent")
    @Mapping(target = "remainingBudget", source = "remainingBudget")
    BudgetResponse toBudgetResponse(Budget budget, BigDecimal totalSpent, BigDecimal remainingBudget);
}
