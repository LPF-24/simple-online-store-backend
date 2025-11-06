package com.simple_online_store_backend.util.seeders;

import com.simple_online_store_backend.entity.Order;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.repository.OrderRepository;
import com.simple_online_store_backend.repository.PeopleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

// @ConditionalOnProperty(value = "demo.helpers.enabled", havingValue = "true")
@Component
@org.springframework.core.annotation.Order(4)
class DemoOrderSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoOrderSeeder.class);

    private final PeopleRepository peopleRepository;
    private final OrderRepository orderRepository;

    DemoOrderSeeder(PeopleRepository peopleRepository, OrderRepository orderRepository) {
        this.peopleRepository = peopleRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public void run(String... args) {
        Person blocked = peopleRepository.findFirstByEmail("blocked@example.com").orElse(null);
        Person user = peopleRepository.findFirstByEmail("user@example.com").orElse(null);
        if (blocked == null || user == null) return;

        boolean anyExists =
                orderRepository.existsByPerson_Id(blocked.getId()) ||
                        orderRepository.existsByPerson_Id(user.getId());

        if (anyExists) return;

        int id1 = createOrder(blocked, OrderStatus.PENDING);   // для проверки 423 в соответствующем эндпоинте
        log.info("[DEMO] Seeded order id={} for blocked user {}", id1, blocked.getEmail());

        int id2 = createOrder(user, OrderStatus.PENDING);      // /orders/{id}/cancel-order → 200
        log.info("[DEMO] Seeded PENDING order id={} for {}", id2, user.getEmail());

        int id3 = createOrder(user, OrderStatus.SHIPPED);      // /orders/{id}/cancel-order → 400
        log.info("[DEMO] Seeded SHIPPED order id={} for {}", id3, user.getEmail());

        int id4 = createOrder(user, OrderStatus.CANCELLED);      // /orders/{id}/reactivate-order → 200
        log.info("[DEMO] Seeded CANCELLED order id={} for {}", id4, user.getEmail());
    }

    private int createOrder(Person owner, OrderStatus status) {
        Order o = new Order();
        o.setPerson(owner);
        o.setStatus(status);
        o.setProducts(new ArrayList<>());
        orderRepository.save(o);
        return o.getId();
    }
}

