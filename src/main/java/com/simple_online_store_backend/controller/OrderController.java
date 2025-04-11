package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.order.OrderRequestDTO;
import com.simple_online_store_backend.dto.order.OrderResponseDTO;
import com.simple_online_store_backend.exception.ErrorUtil;
import com.simple_online_store_backend.service.AdminService;
import com.simple_online_store_backend.service.OrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/all-my-orders")
    public ResponseEntity<List<OrderResponseDTO>> allOrdersByCustomer() {
        List<OrderResponseDTO> listOrders = orderService.findAllOrdersByCustomer();
        return ResponseEntity.ok(listOrders);
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrder(@PathVariable("id") int orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/create-order")
    public ResponseEntity<OrderResponseDTO> addOrder(@RequestBody @Valid OrderRequestDTO dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            ErrorUtil.returnErrorsToClient(bindingResult);
        }

        OrderResponseDTO response = orderService.createOrder(dto);
        return ResponseEntity.ok(response);
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping()
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        return ResponseEntity.ok(adminService.findAllOrders());
    }

    @SecurityRequirement(name = "bearerAuth")
    @RequestMapping(value = "/{id}/cancel-order", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<OrderResponseDTO> cancelOrder(@PathVariable("id") int orderId) {
        OrderResponseDTO response = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(response);
    }

    @SecurityRequirement(name = "bearerAuth")
    @RequestMapping(value = "/{id}/reactivate-order", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<OrderResponseDTO> reactivateOrder(@PathVariable("id") int orderId) {
        OrderResponseDTO response = orderService.reactivateOrder(orderId);
        return ResponseEntity.ok(response);
    }
}
