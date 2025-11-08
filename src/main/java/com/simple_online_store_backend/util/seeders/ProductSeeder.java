package com.simple_online_store_backend.util.seeders;

import com.simple_online_store_backend.entity.Address;
import com.simple_online_store_backend.entity.PickupLocation;
import com.simple_online_store_backend.entity.Product;
import com.simple_online_store_backend.enums.ProductCategory;
import com.simple_online_store_backend.repository.AddressRepository;
import com.simple_online_store_backend.repository.PickupLocationRepository;
import com.simple_online_store_backend.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
// import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;

@Component
//@Profile("dev")
@Order(3)
public class ProductSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ProductSeeder.class);

    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final PickupLocationRepository pickupLocationRepository;

    public ProductSeeder(ProductRepository productRepository,
                         AddressRepository addressRepository,
                         PickupLocationRepository pickupLocationRepository) {
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.pickupLocationRepository = pickupLocationRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedProducts();
        seedAddress();
        seedPickupLocation();
    }

    private void seedProducts() {
        ensureProduct("Phone", "Phone desc", ProductCategory.COMPONENTS, new BigDecimal("499.99"));
        ensureProduct("Case", "Case desc", ProductCategory.ACCESSORIES, new BigDecimal("19.99"));
    }

    private void ensureProduct(String name, String desc, ProductCategory cat, BigDecimal price) {
        if (productRepository.findByProductName(name).isEmpty()) {
            Product p = new Product();
            p.setProductName(name);
            p.setProductDescription(desc);
            p.setProductCategory(cat);
            p.setPrice(price);
            p.setAvailability(true);
            productRepository.save(p);
            log.info("[Seeder] Added product: {}", name);
        } else {
            log.info("[Seeder] Product already exists: {}", name);
        }
    }

    private void seedAddress() {
        ensureAddress("Berlin", "Main Street", "12A", "45", "10115");
    }

    private void ensureAddress(String city, String street, String houseNumber, String apartment, String postalCode) {
        boolean exists = addressRepository.findAll().stream().anyMatch(a ->
                city.equals(a.getCity())
                        && street.equals(a.getStreet())
                        && houseNumber.equals(a.getHouseNumber())
                        && ((apartment == null && a.getApartment() == null) || (apartment != null && apartment.equals(a.getApartment())))
                        && postalCode.equals(a.getPostalCode())
        );
        if (exists) {
            log.info("[Seeder] Address already exists: {}, {} {} (apt {}), {}", city, street, houseNumber, apartment, postalCode);
            return;
        }
        Address addr = new Address();
        addr.setCity(city);
        addr.setStreet(street);
        addr.setHouseNumber(houseNumber);
        addr.setApartment(apartment);
        addr.setPostalCode(postalCode);
        addressRepository.save(addr);
        log.info("[Seeder] Added default address: {}, {} {} (apt {}), {}", city, street, houseNumber, apartment, postalCode);
    }

    private void seedPickupLocation() {
        ensurePickup("Berlin", "Alexanderplatz", "1", true);
    }

    private void ensurePickup(String city, String street, String houseNumber, boolean active) {
        boolean exists = pickupLocationRepository.findAll().stream().anyMatch(p ->
                city.equals(p.getCity())
                        && street.equals(p.getStreet())
                        && houseNumber.equals(p.getHouseNumber())
        );
        if (exists) {
            log.info("[Seeder] Pickup location already exists: {}, {} {}", city, street, houseNumber);
            return;
        }
        PickupLocation pickup = new PickupLocation();
        pickup.setCity(city);
        pickup.setStreet(street);
        pickup.setHouseNumber(houseNumber);
        pickup.setActive(active);
        pickupLocationRepository.save(pickup);
        log.info("[Seeder] Added default pickup location: {}, {} {}", city, street, houseNumber);
    }
}