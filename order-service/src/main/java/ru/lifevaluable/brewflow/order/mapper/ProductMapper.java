package ru.lifevaluable.brewflow.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.lifevaluable.brewflow.order.dto.ProductResponse;
import ru.lifevaluable.brewflow.order.entity.Product;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMapper {
    ProductResponse toDTO(Product product);
}
