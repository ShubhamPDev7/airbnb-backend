package com.codingshuttle.projects.airbnbapp.service;


import com.codingshuttle.projects.airbnbapp.dto.HotelDto;
import com.codingshuttle.projects.airbnbapp.entity.Hotel;

public interface HotelService {
    HotelDto createNewHotel(HotelDto hotelDto);

    HotelDto getHotelById(Long id);

}
