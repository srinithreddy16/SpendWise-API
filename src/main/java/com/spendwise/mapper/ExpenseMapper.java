package com.spendwise.mapper;

import com.spendwise.domain.entity.Expense;
import com.spendwise.dto.response.ExpenseResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Expense entity to ExpenseResponse DTO.
 * No business logic; maps nested category to categoryId.
 */
@Mapper(componentModel = "spring")
public interface ExpenseMapper {

    @Mapping(target = "categoryId", source = "category.id")
    ExpenseResponse toExpenseResponse(Expense expense);
}
