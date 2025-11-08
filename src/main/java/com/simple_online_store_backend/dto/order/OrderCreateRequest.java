package com.simple_online_store_backend.dto.order;

import com.simple_online_store_backend.util.SwaggerConstants;
import com.simple_online_store_backend.validation.annotation.ValidDeliveryOption;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OrderCreateRequest", description = "Request to create an order")
@ValidDeliveryOption
public class OrderCreateRequest {

    @NotEmpty(message = "You must add at least one product")
    @ArraySchema(
            arraySchema = @Schema(
                    description = SwaggerConstants.ORDER_PRODUCT_IDS_DESC,
                    example = SwaggerConstants.ORDER_PRODUCT_IDS_EXAMPLE
            ),
            schema = @Schema(
                    description = "ID продукта",
                    minimum = "1"
            ),
            minItems = 1
    )
    private List<@NotNull @Min(1) Integer> productIds;

    @Schema(
            description = SwaggerConstants.ORDER_ADDRESS_ID_DESC,
            example = SwaggerConstants.ID_EXAMPLE,
            minimum = "1",
            nullable = true
    )
    private Integer addressId;

    @Schema(
            description = SwaggerConstants.ORDER_PICKUP_LOCATION_ID_DESC,
            example = SwaggerConstants.ID_EXAMPLE,
            minimum = "1",
            nullable = true
    )
    private Integer pickupLocationId;

    public List<Integer> getProductIds() { return productIds; }
    public void setProductIds(List<Integer> productIds) { this.productIds = productIds; }
    public Integer getAddressId() { return addressId; }
    public void setAddressId(Integer addressId) { this.addressId = addressId; }
    public Integer getPickupLocationId() { return pickupLocationId; }
    public void setPickupLocationId(Integer pickupLocationId) { this.pickupLocationId = pickupLocationId; }
}
