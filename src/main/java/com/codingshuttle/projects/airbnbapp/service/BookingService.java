package com.codingshuttle.projects.airbnbapp.service;

import com.codingshuttle.projects.airbnbapp.dto.BookingDto;
import com.codingshuttle.projects.airbnbapp.dto.BookingRequest;
import com.codingshuttle.projects.airbnbapp.dto.GuestDto;

import java.util.List;

public interface BookingService {


    BookingDto initialiseBooking(BookingRequest bookingRequest);

    BookingDto addGuests(Long bookingId, List<GuestDto> guestDtoList);
}
