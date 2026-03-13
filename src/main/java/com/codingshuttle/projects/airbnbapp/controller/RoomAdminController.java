package com.codingshuttle.projects.airbnbapp.controller;

import com.codingshuttle.projects.airbnbapp.dto.RoomDto;
import com.codingshuttle.projects.airbnbapp.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/hotels/{hotelId}/rooms")
@RequiredArgsConstructor
public class RoomAdminController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomDto> createNewRoom(@RequestBody RoomDto roomDto,
                                                 @PathVariable Long hotelId) {
        RoomDto room = roomService.createNewRoom(hotelId, roomDto);
        return new ResponseEntity<>(room, HttpStatus.CREATED);

    }

    @GetMapping
    public ResponseEntity<List<RoomDto>> getALlRoomsInHotel(@PathVariable Long hotelId) {
        return ResponseEntity.ok(roomService.getAllRoomsInHotel(hotelId));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDto> getRoomById(@PathVariable Long roomId, @PathVariable Long hotelId) {
        return ResponseEntity.ok(roomService.getRoomById(roomId));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoomById(@PathVariable Long roomId, @PathVariable Long hotelId) {
        roomService.deleteRoomById(roomId);
        return ResponseEntity.noContent().build();
    }

}
