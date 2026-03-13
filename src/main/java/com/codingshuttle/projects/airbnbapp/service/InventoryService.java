package com.codingshuttle.projects.airbnbapp.service;

import com.codingshuttle.projects.airbnbapp.entity.Room;

public interface InventoryService {

    void initializeRoomForAYear(Room room);

    void deleteFutureInventories(Room room);

}
