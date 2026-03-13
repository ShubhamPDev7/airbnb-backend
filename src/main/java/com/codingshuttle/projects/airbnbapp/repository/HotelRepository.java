package com.codingshuttle.projects.airbnbapp.repository;

import com.codingshuttle.projects.airbnbapp.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {
    Long id(Long id);
}
