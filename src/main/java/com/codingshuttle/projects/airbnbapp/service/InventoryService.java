package com.codingshuttle.projects.airbnbapp.service;

import com.codingshuttle.projects.airbnbapp.dto.HotelDto;
import com.codingshuttle.projects.airbnbapp.dto.HotelSearchRequest;
import com.codingshuttle.projects.airbnbapp.entity.Room;
import org.springframework.data.domain.Page;

public interface InventoryService {

    void initializeRoomForAYear(Room room);

    void deleteAllInventories(Room room);

    Page<HotelDto> searchHotels(HotelSearchRequest hotelSearchRequest);
}
