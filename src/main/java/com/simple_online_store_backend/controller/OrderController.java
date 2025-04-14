package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.order.OrderRequestDTO;
import com.simple_online_store_backend.dto.order.OrderResponseDTO;
import com.simple_online_store_backend.exception.ErrorUtil;
import com.simple_online_store_backend.service.AdminService;
import com.simple_online_store_backend.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Order", description = "Endpoints for work with orders")
@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;
    private final AdminService adminService;

    @Autowired
    public OrderController(OrderService orderService, AdminService adminService) {
        this.orderService = orderService;
        this.adminService = adminService;
    }

    @Operation(summary = "Shows the list of orders placed by the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "List successfully received")
    @ApiResponse(responseCode = "500", description = "Error inside method")
    @ApiResponse(responseCode = "403", description = "User is authenticated but not allowed to access this resource")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/all-my-orders")
    public ResponseEntity<List<OrderResponseDTO>> allOrdersByCustomer() {
        List<OrderResponseDTO> listOrders = orderService.findAllOrdersByCustomer();
        return ResponseEntity.ok(listOrders);
    }

    @Operation(summary = "Returns an order by unique ID", description = "Allows user to view information about a specific order")
    @ApiResponse(responseCode = "200", description = "Order successfully received")
    @ApiResponse(responseCode = "500", description = "Error inside method")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrder(@PathVariable("id") int orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @Operation(summary = "Add an order", description = "Adds an order for the user")
    @ApiResponse(responseCode = "200", description = "Order successfully added")
    @ApiResponse(responseCode = "500", description = "Error inside method")
    @ApiResponse(responseCode = "403", description = "User is authenticated but not allowed to access this resource")
    @ApiResponse(responseCode = "400", description = "Request is invalid or missing required parameters")
    @ApiResponse(responseCode = "422", description = "Validation failed on request data")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/create-order")
    public ResponseEntity<OrderResponseDTO> addOrder(@RequestBody @Valid OrderRequestDTO dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            ErrorUtil.returnErrorsToClient(bindingResult);
        }

        OrderResponseDTO response = orderService.createOrder(dto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Shows a list of orders for all users")
    @ApiResponse(responseCode = "200", description = "List successfully received")
    @ApiResponse(responseCode = "500", description = "Error inside method")
    @ApiResponse(responseCode = "403", description = "User is authenticated but not allowed to access this resource")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping()
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        return ResponseEntity.ok(adminService.findAllOrders());
    }

    @Operation(summary = "Cancel an order", description = "Changes the order status to CANCELLED")
    @ApiResponse(responseCode = "200", description = "Order successfully cancelled")
    @ApiResponse(responseCode = "500", description = "Error inside method")
    @ApiResponse(responseCode = "403", description = "User is authenticated but not allowed to access this resource")
    @SecurityRequirement(name = "bearerAuth")
    @RequestMapping(value = "/{id}/cancel-order", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<OrderResponseDTO> cancelOrder(@PathVariable("id") int orderId) {
        OrderResponseDTO response = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reactivate an order", description = "Changes the order status to PENDING")
    @ApiResponse(responseCode = "200", description = "Order successfully reactivated")
    @ApiResponse(responseCode = "500", description = "Error inside method")
    @ApiResponse(responseCode = "403", description = "User is authenticated but not allowed to access this resource")
    @SecurityRequirement(name = "bearerAuth")
    @RequestMapping(value = "/{id}/reactivate-order", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<OrderResponseDTO> reactivateOrder(@PathVariable("id") int orderId) {
        OrderResponseDTO response = orderService.reactivateOrder(orderId);
        return ResponseEntity.ok(response);
    }
}
