package com.codingshuttle.projects.airbnbapp.service;

import com.codingshuttle.projects.airbnbapp.dto.RoomDto;
import com.codingshuttle.projects.airbnbapp.entity.Room;

import java.util.List;

public interface RoomService {

    RoomDto createNewRoom(Long hotelId, RoomDto roomDto);

    List<RoomDto> getAllRoomsInHotel(Long hotelId);

    RoomDto getRoomById(Long roomId);

    void deleteRoomById(Long roomId);


}
