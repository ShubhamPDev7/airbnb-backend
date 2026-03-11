package com.codingshuttle.projects.airbnbapp.repository;

import com.codingshuttle.projects.airbnbapp.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
