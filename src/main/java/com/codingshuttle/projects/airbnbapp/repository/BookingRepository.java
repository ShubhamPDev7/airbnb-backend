package com.codingshuttle.projects.airbnbapp.repository;


import com.codingshuttle.projects.airbnbapp.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}
