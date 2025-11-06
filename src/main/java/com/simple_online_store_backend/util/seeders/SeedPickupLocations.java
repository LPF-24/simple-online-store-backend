package com.simple_online_store_backend.bootstrap;

import com.simple_online_store_backend.entity.PickupLocation;
import com.simple_online_store_backend.repository.PickupLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds two pickup locations on application startup (non-test profiles):
 *  - Berlin, Main 1A (active)
 *  - Munich, Kaufingerstr. 12 (inactive)
 *
 * Idempotent: checks for existence before inserting.
 */
@Component
@Order(100)
@Profile("!test") // не запускаем сидер в тестовом окружении
public class SeedPickupLocations implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedPickupLocations.class);

    private final PickupLocationRepository pickupLocationRepository;

    public SeedPickupLocations(PickupLocationRepository pickupLocationRepository) {
        this.pickupLocationRepository = pickupLocationRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seed("Berlin", "Main", "1A", true);
        seed("Munich", "Kaufingerstr.", "12", false);
    }

    private void seed(String city, String street, String house, boolean active) {
        boolean exists;
        try {
            exists = pickupLocationRepository
                    .existsByCityIgnoreCaseAndStreetIgnoreCaseAndHouseNumberIgnoreCase(city, street, house);
        } catch (Exception e) {
            exists = pickupLocationRepository.findAll().stream()
                    .anyMatch(pl -> pl.getCity().equalsIgnoreCase(city)
                            && pl.getStreet().equalsIgnoreCase(street)
                            && pl.getHouseNumber().equalsIgnoreCase(house));
        }

        if (exists) {
            log.debug("Pickup location already present: {}, {} {}", city, street, house);
            return;
        }

        PickupLocation pl = new PickupLocation();
        pl.setCity(city);
        pl.setStreet(street);
        pl.setHouseNumber(house);
        pl.setActive(active);

        pickupLocationRepository.save(pl);
        log.info("Seeded pickup location: {}, {} {} (active={})", city, street, house, active);
    }
}
