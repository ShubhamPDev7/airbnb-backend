package com.codingshuttle.projects.airBnbApp.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class HotelSearchRequest {

    private String city;
    private LocalDate startDate = LocalDate.now();
    private LocalDate endDate = LocalDate.now().plusDays(1);
    private Integer roomsCount;

    private Integer page=0;
    private Integer size=10;
}
