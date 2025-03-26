package com.simple_online_store_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "pickup_locations")
@NoArgsConstructor
@Getter
@Setter
public class PickupLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String street;

    @Column(name = "house_number", nullable = false, length = 10)
    private String houseNumber;

    @OneToMany(mappedBy = "pickupLocation", fetch = FetchType.LAZY)
    private List<Order> orderList;
}
