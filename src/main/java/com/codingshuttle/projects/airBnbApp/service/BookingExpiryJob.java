package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.entity.Booking;
import com.codingshuttle.projects.airBnbApp.entity.enums.BookingStatus;
import com.codingshuttle.projects.airBnbApp.repository.BookingRepository;
import com.codingshuttle.projects.airBnbApp.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingExpiryJob {

    private final BookingRepository bookingRepository;
    private final InventoryRepository inventoryRepository;

    @Scheduled(cron = "0 */10 * * * *")
    public void expireStaleBookings() {

        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(10);

        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(
                BookingStatus.RESERVED,
                expiryTime
        );

        if (expiredBookings.isEmpty()) {
            log.info("No stale bookings found to expire");
            return;
        }

        log.info("Found {} stale bookings to expire", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            inventoryRepository.releaseReservedInventory(
                    booking.getRoom().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    booking.getRoomsCount()
            );

            booking.setBookingStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);

            log.info("Expired booking ID: {}", booking.getId());
        }
    }

}
