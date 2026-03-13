package com.codingshuttle.projects.airbnbapp.service;

import com.codingshuttle.projects.airbnbapp.dto.RoomDto;
import com.codingshuttle.projects.airbnbapp.entity.Hotel;
import com.codingshuttle.projects.airbnbapp.entity.Room;
import com.codingshuttle.projects.airbnbapp.exception.ResourceNotFoundException;
import com.codingshuttle.projects.airbnbapp.repository.HotelRepository;
import com.codingshuttle.projects.airbnbapp.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;

    @Override
    public RoomDto createNewRoom(Long hotelId, RoomDto roomDto) {
        log.info("Creating a new room in hotel with ID: {}", hotelId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel with ID: " + hotelId + " not found"));
        Room room = modelMapper.map(roomDto, Room.class);
        room.setHotel(hotel);
        room = roomRepository.save(room);
        //        TODO: create inventory as soon as room is created and if hotel is active
        return modelMapper.map(room, RoomDto.class);
    }

    @Override
    public List<RoomDto> getAllRoomsInHotel(Long hotelId) {
        log.info("Getting all rooms in hotel with ID: {}", hotelId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel with ID: " + hotelId + " not found"));
        return hotel.getRooms()
                .stream()
                .map((element) -> modelMapper.map(element, RoomDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public RoomDto getRoomById(Long roomId) {
        log.info("Getting the room with ID: {}", roomId);
        Room room = roomRepository
                .findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room with ID: " + roomId + " not found"));
        return modelMapper.map(room, RoomDto.class);

    }

    @Override
    public void deleteRoomById(Long roomId) {
        log.info("Deleting the room with ID: {}", roomId);
        boolean exists = roomRepository.existsById(roomId);
        if (!exists) {
            throw new ResourceNotFoundException("Room with ID: " + roomId + " not found");
        }
        roomRepository.deleteById(roomId);

//        TODO: Delete all future inventories for this room
    }
}
