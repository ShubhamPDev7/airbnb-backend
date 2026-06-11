package com.codingshuttle.projects.airBnbApp.repository;

import com.codingshuttle.projects.airBnbApp.dto.HotelPriceDto;
import com.codingshuttle.projects.airBnbApp.entity.Hotel;
import com.codingshuttle.projects.airBnbApp.entity.HotelMinPrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface HotelMinPriceRepository extends JpaRepository<HotelMinPrice, Long> {

    @Query(value = """
        SELECT new com.codingshuttle.projects.airBnbApp.dto.HotelPriceDto(i.hotel, AVG(i.price))
        FROM HotelMinPrice i
        WHERE (:city = '' OR i.hotel.city = :city)
            AND i.date BETWEEN :startDate AND :endDate
            AND i.hotel.active = true
        GROUP BY i.hotel
        """,
            countQuery = """
        SELECT COUNT(DISTINCT i.hotel)
        FROM HotelMinPrice i
        WHERE (:city = '' OR i.hotel.city = :city)
            AND i.date BETWEEN :startDate AND :endDate
            AND i.hotel.active = true
        """)
    Page<HotelPriceDto> findHotelsWithAvailableInventory(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Integer roomsCount,
            @Param("dateCount") Long dateCount,
            Pageable pageable
    );

    Optional<HotelMinPrice> findByHotelAndDate(Hotel hotel, LocalDate date);

    void deleteByHotel(Hotel hotel);
}