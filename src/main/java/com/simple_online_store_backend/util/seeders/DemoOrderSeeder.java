package com.simple_online_store_backend.util.seeders;

import com.simple_online_store_backend.entity.Order;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.repository.OrderRepository;
import com.simple_online_store_backend.repository.PeopleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// @ConditionalOnProperty(value = "demo.helpers.enabled", havingValue = "true")
@Component
class DemoOrderSeeder implements CommandLineRunner {

    private final PeopleRepository peopleRepository;
    private final OrderRepository orderRepository;

    DemoOrderSeeder(PeopleRepository peopleRepository, OrderRepository orderRepository) {
        this.peopleRepository = peopleRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public void run(String... args) {
        Person owner = peopleRepository.findFirstByEmail("blocked@example.com").orElse(null);
        Person other = peopleRepository.findFirstByEmail("user@example.com").orElse(null);

        if (owner == null || other == null) return;

        boolean exists = orderRepository.existsByPersonId(owner.getId());
        if (!exists) {
            Order o = new Order();
            o.setPerson(owner);
            o.setStatus(OrderStatus.PENDING);
            orderRepository.save(o);
            System.out.println("[DEMO] Seeded foreign order id=" + o.getId() + " for user=" + owner.getEmail());
        }
    }
}

