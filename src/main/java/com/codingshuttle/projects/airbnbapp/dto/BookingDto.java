package com.codingshuttle.projects.airbnbapp.dto;

import com.codingshuttle.projects.airbnbapp.entity.Guest;
import com.codingshuttle.projects.airbnbapp.entity.Hotel;
import com.codingshuttle.projects.airbnbapp.entity.Room;
import com.codingshuttle.projects.airbnbapp.entity.User;
import com.codingshuttle.projects.airbnbapp.entity.enums.BookingStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class BookingDto {
    private Long id;
    private Integer roomsCount;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BookingStatus bookingStatus;
    private Set<GuestDto> guests;
}
