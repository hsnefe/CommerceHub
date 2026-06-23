package com.commercehub.product.dto;

import java.util.List;

public record ProductPageResponse(
        List<ProductSummaryResponse> content,
        int page,
        int size,
        long totalElements
) {
}
