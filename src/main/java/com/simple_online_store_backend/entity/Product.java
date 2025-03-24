package com.simple_online_store_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@NoArgsConstructor
@Getter
@Setter
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_description", nullable = false, length = 600)
    private String productDescription;

    //максимум 10 цифр, из них 2 после запятой
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "product_category", nullable = false, length = 100)
    private String productCategory;

    @Column(nullable = false)
    private Boolean availability;

    @ManyToMany(mappedBy = "products")
    private List<Order> orders = new ArrayList<>();
}
