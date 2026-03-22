package com.codingshuttle.projects.airbnbapp.repository;

import com.codingshuttle.projects.airbnbapp.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, Long> {
}