package ru.lifevaluable.brewflow.order.dto;

import java.util.List;

public record OrdersHistoryResponse(
        List<OrderHistoryResponse> orders
) {}
