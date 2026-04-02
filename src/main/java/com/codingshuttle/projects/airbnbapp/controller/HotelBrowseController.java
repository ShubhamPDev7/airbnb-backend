package com.codingshuttle.projects.airbnbapp.controller;

import com.codingshuttle.projects.airbnbapp.dto.HotelDto;
import com.codingshuttle.projects.airbnbapp.dto.HotelInfoDto;
import com.codingshuttle.projects.airbnbapp.dto.HotelPriceDto;
import com.codingshuttle.projects.airbnbapp.dto.HotelSearchRequest;
import com.codingshuttle.projects.airbnbapp.service.HotelService;
import com.codingshuttle.projects.airbnbapp.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
public class HotelBrowseController {

    private final InventoryService  inventoryService;
    private final HotelService hotelService;

    @GetMapping("/search")
    public ResponseEntity<Page<HotelPriceDto>> searchHotels(@RequestBody HotelSearchRequest hotelSearchRequest){
        var page = inventoryService.searchHotels(hotelSearchRequest);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{hotelId}/info")
    public ResponseEntity<HotelInfoDto> getHotelInfo(@PathVariable Long hotelId){
        return ResponseEntity.ok(hotelService.getHotelInfoById(hotelId));
    }
}
