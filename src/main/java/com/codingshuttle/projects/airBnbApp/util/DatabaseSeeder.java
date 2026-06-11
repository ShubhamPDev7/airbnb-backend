package com.codingshuttle.projects.airBnbApp.util;

import com.codingshuttle.projects.airBnbApp.entity.Hotel;
import com.codingshuttle.projects.airBnbApp.entity.HotelContactInfo;
import com.codingshuttle.projects.airBnbApp.entity.Room;
import com.codingshuttle.projects.airBnbApp.entity.User;
import com.codingshuttle.projects.airBnbApp.repository.HotelRepository;
import com.codingshuttle.projects.airBnbApp.repository.RoomRepository;
import com.codingshuttle.projects.airBnbApp.repository.UserRepository;
import com.codingshuttle.projects.airBnbApp.service.InventoryService;
import com.codingshuttle.projects.airBnbApp.service.PricingUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Random;

//@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final InventoryService inventoryService;
    private final PricingUpdateService pricingUpdateService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Safety check: Don't seed if we already have a bunch of hotels
        if (hotelRepository.count() >= 20) {
            log.info("Database is already populated. Skipping seeder.");
            return;
        }

        // 2. Grab the first user in your database to act as the "Host"
        User owner = userRepository.findAll().stream().findFirst().orElse(null);
        if (owner == null) {
            log.warn("Cannot seed database: No users exist! Please register an account first.");
            return;
        }

        log.info("🚀 Starting massive database seed... Generating 20 Hotels!");

        String[] cities = {"Pune", "Mumbai", "Delhi", "Goa", "Bangalore", "Manali", "Jaipur", "Kochi"};
        String[] prefixes = {"Luxury", "Cozy", "Grand", "Boutique", "Urban", "Seaside", "Mountain", "Historic", "Modern"};
        String[] suffixes = {"Villa", "Loft", "Resort", "Retreat", "Palace", "Inn", "Suites", "Hideaway", "Cabin"};

        Random random = new Random();

        for (int i = 1; i <= 20; i++) {
            String city = cities[random.nextInt(cities.length)];
            String name = prefixes[random.nextInt(prefixes.length)] + " " + suffixes[random.nextInt(suffixes.length)] + " " + city;

            // Create Hotel
            Hotel hotel = new Hotel();
            hotel.setName(name);
            hotel.setCity(city);
            hotel.setActive(true);
            hotel.setOwner(owner);

            // Using our bulletproof Unsplash fallbacks
            String[] photos = {
                    "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80",
                    "https://images.unsplash.com/photo-1542314831-c53cd4b85d05?auto=format&fit=crop&w=800&q=80",
                    "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=800&q=80",
                    "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80"
            };
            hotel.setPhotos(photos);

            String[] amenities = {"WiFi", "Air Conditioning", "Kitchen", "Free Parking", "Pool"};
            hotel.setAmenities(amenities);

            // Optional: Set contact info if your Embeddable class requires it
            HotelContactInfo contact = new HotelContactInfo();
            contact.setAddress(random.nextInt(900) + 100 + " Main Street");
            contact.setPhoneNumber("+91 9876543" + String.format("%03d", i));
            contact.setEmail("contact@" + name.replaceAll("\\s", "").toLowerCase() + ".com");
            hotel.setContactInfo(contact);

            // Save the Hotel to DB
            hotel = hotelRepository.save(hotel);

            // Create 1 to 3 Rooms for this hotel
            int numRooms = random.nextInt(3) + 1;
            for (int r = 0; r < numRooms; r++) {
                Room room = new Room();
                room.setHotel(hotel);
                room.setType(r == 0 ? "Deluxe Master Suite" : "Standard Double Room");
                // Random price between 1500 and 9500
                room.setBasePrice(BigDecimal.valueOf(1500 + random.nextInt(8000)));
                room.setTotalCount(random.nextInt(4) + 1);

                room.setCapacity(random.nextInt(4) + 1);

                room = roomRepository.save(room);

                // 🌟 Trigger your heavy business logic to generate 365 days of inventory
                inventoryService.initializeRoomForAYear(room);
            }

            // Trigger pricing aggregation
            pricingUpdateService.updatePricesForHotel(hotel);
            log.info("✅ Seeded [" + i + "/20]: " + name + " in " + city);
        }

        log.info("🎉 Database seeding complete! 20 Hotels and thousands of inventory rows generated.");
    }
}
