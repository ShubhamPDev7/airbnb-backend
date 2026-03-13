package com.codingshuttle.projects.airbnbapp.repository;

import com.codingshuttle.projects.airbnbapp.entity.Inventory;
import com.codingshuttle.projects.airbnbapp.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface InventoryRepository extends JpaRepository<Inventory,Long> {

    void deleteByDateAfterAndRoom(LocalDate date, Room room);
}
