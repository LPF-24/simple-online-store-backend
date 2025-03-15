package com.simple_online_store_backend.entity;

import com.simple_online_store_backend.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;

@Entity
@Table(name = "orders")
@NoArgsConstructor
@Getter
@Setter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToMany(mappedBy = "order", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<Product> productList;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @ManyToOne
    @JoinColumn(name = "person_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Person person;

    @ManyToOne
    @JoinColumn(name = "pickup_location_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private PickupLocation pickupLocation;

    @ManyToOne
    @JoinColumn(name = "address_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Address address;
}

