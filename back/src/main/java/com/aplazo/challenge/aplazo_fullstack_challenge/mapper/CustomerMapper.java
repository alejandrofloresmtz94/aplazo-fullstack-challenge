package com.aplazo.challenge.aplazo_fullstack_challenge.mapper;

import java.time.Instant;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer.CustomerRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer.CustomerResponse;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.CustomerEntity;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    // Request → Entity
    @Mapping(target = "id", ignore = true) // ID se genera en DB
    @Mapping(target = "creditLineAmount", expression = "java(new java.math.BigDecimal(\"1000.00\"))")
    @Mapping(target = "availableCreditLineAmount", expression = "java(new java.math.BigDecimal(\"1000.00\"))")
    @Mapping(target = "createdAt", ignore = true)
    CustomerEntity toEntity(CustomerRequest request);

    // Entity → Response
    @Mapping(source = "createdAt", target = "createdAt", resultType = Instant.class)
    CustomerResponse toResponse(CustomerEntity entity);
}
